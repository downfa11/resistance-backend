package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"github.com/downfa11/resistance-backend/internal/resistance/domain"
	modernsqlite "modernc.org/sqlite"
)

type ExchangeRecord struct {
	Exchange    domain.Exchange
	RequestHash string
}

func (r *Repository) Rates(ctx context.Context) ([]domain.CurrencyRate, error) {
	rows, err := r.db.QueryContext(ctx, `SELECT code, rate, uses, updated_at FROM resistance_currency_rates ORDER BY code`)
	if err != nil {
		return nil, fmt.Errorf("list resistance rates: %w", err)
	}
	defer rows.Close()
	items := make([]domain.CurrencyRate, 0)
	for rows.Next() {
		var item domain.CurrencyRate
		var updated string
		if err := rows.Scan(&item.Code, &item.Rate, &item.Uses, &updated); err != nil {
			return nil, err
		}
		item.UpdatedAt, err = parseTime(updated)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) AdjustRates(ctx context.Context, now time.Time) ([]domain.CurrencyRate, error) {
	items, err := r.Rates(ctx)
	if err != nil {
		return nil, err
	}
	var total int64
	for _, item := range items {
		total += item.Uses
	}
	for _, item := range items {
		next := domain.AdjustedRate(item.Rate, item.Uses, total)
		if _, err := r.db.ExecContext(ctx, `UPDATE resistance_currency_rates SET rate = ?, uses = 0, updated_at = ? WHERE code = ?`, next, formatTime(now), item.Code); err != nil {
			return nil, fmt.Errorf("adjust resistance rate %s: %w", item.Code, err)
		}
	}
	return r.Rates(ctx)
}

func (r *Repository) ResetRates(ctx context.Context, now time.Time) ([]domain.CurrencyRate, error) {
	for code, rate := range domain.DefaultCurrencyRates {
		if _, err := r.db.ExecContext(ctx, `INSERT INTO resistance_currency_rates (code, rate, uses, updated_at) VALUES (?, ?, 0, ?) ON CONFLICT(code) DO UPDATE SET rate = excluded.rate, uses = 0, updated_at = excluded.updated_at`, code, rate, formatTime(now)); err != nil {
			return nil, fmt.Errorf("reset resistance rate %s: %w", code, err)
		}
	}
	return r.Rates(ctx)
}

func (r *Repository) Balances(ctx context.Context, userID int64) ([]domain.CurrencyBalance, error) {
	rows, err := r.db.QueryContext(ctx, `SELECT rate.code, COALESCE(balance.quantity, 0) FROM resistance_currency_rates rate LEFT JOIN resistance_currency_balances balance ON balance.code = rate.code AND balance.user_id = ? ORDER BY rate.code`, userID)
	if err != nil {
		return nil, fmt.Errorf("list resistance balances: %w", err)
	}
	defer rows.Close()
	items := make([]domain.CurrencyBalance, 0)
	for rows.Next() {
		var item domain.CurrencyBalance
		if err := rows.Scan(&item.Code, &item.Quantity); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) SetBalance(ctx context.Context, userID int64, code string, quantity int64, now time.Time) error {
	_, err := r.db.ExecContext(ctx, `INSERT INTO resistance_currency_balances (user_id, code, quantity, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(user_id, code) DO UPDATE SET quantity = excluded.quantity, updated_at = excluded.updated_at`, userID, code, quantity, formatTime(now))
	if err != nil {
		return fmt.Errorf("set resistance balance: %w", err)
	}
	return nil
}

func (r *Repository) FindExchangeByKey(ctx context.Context, userID int64, key string) (*ExchangeRecord, error) {
	var record ExchangeRecord
	var created string
	err := r.db.QueryRowContext(ctx, `SELECT id, user_id, currency_code, quantity, rate, gold_granted, idempotency_key, request_hash, created_at FROM resistance_exchange_transactions WHERE user_id = ? AND idempotency_key = ?`, userID, key).
		Scan(&record.Exchange.ID, &record.Exchange.UserID, &record.Exchange.CurrencyCode, &record.Exchange.Quantity, &record.Exchange.Rate, &record.Exchange.GoldGranted, &record.Exchange.IdempotencyKey, &record.RequestHash, &created)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("find resistance exchange: %w", err)
	}
	record.Exchange.CreatedAt, err = parseTime(created)
	if err != nil {
		return nil, err
	}
	if err := r.readExchangeBalances(ctx, &record.Exchange); err != nil {
		return nil, err
	}
	return &record, nil
}

func (r *Repository) Exchange(ctx context.Context, userID int64, code string, quantity int64, key, requestHash string, now time.Time) (*domain.Exchange, error) {
	var currentRate int
	var usage, totalUsage int64
	err := r.db.QueryRowContext(ctx, `SELECT rate, uses, (SELECT COALESCE(SUM(uses), 0) FROM resistance_currency_rates) FROM resistance_currency_rates WHERE code = ?`, code).Scan(&currentRate, &usage, &totalUsage)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("read resistance rate: %w", err)
	}
	rate := domain.AdjustedRate(currentRate, usage, totalUsage)
	result, err := r.db.ExecContext(ctx, `UPDATE resistance_currency_balances SET quantity = quantity - ?, updated_at = ? WHERE user_id = ? AND code = ? AND quantity >= ?`, quantity, formatTime(now), userID, code, quantity)
	if err != nil {
		return nil, fmt.Errorf("debit resistance balance: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return nil, domain.ErrInsufficientCurrency
	}
	gold := quantity * int64(rate)
	result, err = r.db.ExecContext(ctx, `UPDATE resistance_profiles SET gold = gold + ?, updated_at = ? WHERE user_id = ? AND gold <= ?`, gold, formatTime(now), userID, domain.MaxGold-gold)
	if err != nil {
		return nil, fmt.Errorf("credit resistance gold: %w", err)
	}
	count, _ = result.RowsAffected()
	if count == 0 {
		return nil, r.profileCreditError(ctx, userID)
	}
	if _, err := r.db.ExecContext(ctx, `UPDATE resistance_currency_rates SET uses = uses + ?, updated_at = ? WHERE code = ?`, quantity, formatTime(now), code); err != nil {
		return nil, fmt.Errorf("update resistance rate usage: %w", err)
	}
	insert, err := r.db.ExecContext(ctx, `INSERT INTO resistance_exchange_transactions (user_id, currency_code, quantity, rate, gold_granted, idempotency_key, request_hash, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`, userID, code, quantity, rate, gold, key, requestHash, formatTime(now))
	if err != nil {
		return nil, fmt.Errorf("insert resistance exchange: %w", err)
	}
	id, _ := insert.LastInsertId()
	item := &domain.Exchange{ID: id, UserID: userID, CurrencyCode: code, Quantity: quantity, Rate: rate, GoldGranted: gold, IdempotencyKey: key, CreatedAt: now}
	if err := r.readExchangeBalances(ctx, item); err != nil {
		return nil, err
	}
	return item, nil
}

func (r *Repository) readExchangeBalances(ctx context.Context, item *domain.Exchange) error {
	err := r.db.QueryRowContext(ctx, `SELECT b.quantity, p.gold FROM resistance_currency_balances b JOIN resistance_profiles p ON p.user_id = b.user_id WHERE b.user_id = ? AND b.code = ?`, item.UserID, item.CurrencyCode).Scan(&item.BalanceAfter, &item.GoldAfter)
	if err != nil {
		return fmt.Errorf("read resistance exchange balances: %w", err)
	}
	return nil
}

func (r *Repository) CreateSupporterCode(ctx context.Context, item *domain.SupporterCode) error {
	result, err := r.db.ExecContext(ctx, `INSERT INTO resistance_supporter_codes (kind, code, reward_gold, status, created_at) VALUES (?, ?, ?, 'AVAILABLE', ?)`, item.Kind, item.Code, item.RewardGold, formatTime(item.CreatedAt))
	if err != nil {
		if isConstraintError(err) {
			return domain.ErrConflict
		}
		return fmt.Errorf("create supporter code: %w", err)
	}
	item.ID, err = result.LastInsertId()
	return err
}

func (r *Repository) SupporterCodes(ctx context.Context, limit int) ([]domain.SupporterCode, error) {
	if limit < 1 || limit > 1000 {
		limit = 200
	}
	rows, err := r.db.QueryContext(ctx, `SELECT id, kind, code, reward_gold, status, redeemed_by, redeemed_at, created_at FROM resistance_supporter_codes ORDER BY id DESC LIMIT ?`, limit)
	if err != nil {
		return nil, fmt.Errorf("list supporter codes: %w", err)
	}
	defer rows.Close()
	items := make([]domain.SupporterCode, 0)
	for rows.Next() {
		var item domain.SupporterCode
		var redeemedBy sql.NullInt64
		var redeemedAt sql.NullString
		var created string
		if err := rows.Scan(&item.ID, &item.Kind, &item.Code, &item.RewardGold, &item.Status, &redeemedBy, &redeemedAt, &created); err != nil {
			return nil, err
		}
		if redeemedBy.Valid {
			item.RedeemedBy = &redeemedBy.Int64
		}
		if redeemedAt.Valid {
			value, err := parseTime(redeemedAt.String)
			if err != nil {
				return nil, err
			}
			item.RedeemedAt = &value
		}
		item.CreatedAt, err = parseTime(created)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) DeleteSupporterCode(ctx context.Context, id int64) error {
	result, err := r.db.ExecContext(ctx, `DELETE FROM resistance_supporter_codes WHERE id = ? AND status = 'AVAILABLE'`, id)
	if err != nil {
		return fmt.Errorf("delete supporter code: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrConflict
	}
	return nil
}

func (r *Repository) SupporterDetails(ctx context.Context) ([]domain.SupporterDetail, error) {
	rows, err := r.db.QueryContext(ctx, `SELECT id, title, details, created_at, updated_at FROM resistance_supporter_details ORDER BY id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]domain.SupporterDetail, 0)
	for rows.Next() {
		var item domain.SupporterDetail
		var created, updated string
		if err := rows.Scan(&item.ID, &item.Title, &item.Details, &created, &updated); err != nil {
			return nil, err
		}
		item.CreatedAt, err = parseTime(created)
		if err != nil {
			return nil, err
		}
		item.UpdatedAt, err = parseTime(updated)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) CreateSupporterDetail(ctx context.Context, item *domain.SupporterDetail) error {
	result, err := r.db.ExecContext(ctx, `INSERT INTO resistance_supporter_details (title, details, created_at, updated_at) VALUES (?, ?, ?, ?)`, item.Title, item.Details, formatTime(item.CreatedAt), formatTime(item.UpdatedAt))
	if err != nil {
		return err
	}
	item.ID, err = result.LastInsertId()
	return err
}

func (r *Repository) UpdateSupporterDetail(ctx context.Context, item domain.SupporterDetail) (*domain.SupporterDetail, error) {
	var created, updated string
	err := r.db.QueryRowContext(ctx, `UPDATE resistance_supporter_details SET title = ?, details = ?, updated_at = ? WHERE id = ? RETURNING id, title, details, created_at, updated_at`, item.Title, item.Details, formatTime(item.UpdatedAt), item.ID).
		Scan(&item.ID, &item.Title, &item.Details, &created, &updated)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	item.CreatedAt, err = parseTime(created)
	if err != nil {
		return nil, err
	}
	item.UpdatedAt, err = parseTime(updated)
	if err != nil {
		return nil, err
	}
	return &item, nil
}

func (r *Repository) DeleteSupporterDetail(ctx context.Context, id int64) error {
	result, err := r.db.ExecContext(ctx, `DELETE FROM resistance_supporter_details WHERE id = ?`, id)
	if err != nil {
		return err
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrNotFound
	}
	return nil
}

func (r *Repository) SupporterCode(ctx context.Context, code string) (*domain.SupporterCode, error) {
	var item domain.SupporterCode
	var redeemedBy sql.NullInt64
	var redeemedAt sql.NullString
	var created string
	err := r.db.QueryRowContext(ctx, `SELECT id, kind, code, reward_gold, status, redeemed_by, redeemed_at, created_at FROM resistance_supporter_codes WHERE code = ?`, code).Scan(&item.ID, &item.Kind, &item.Code, &item.RewardGold, &item.Status, &redeemedBy, &redeemedAt, &created)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("find supporter code: %w", err)
	}
	if redeemedBy.Valid {
		item.RedeemedBy = &redeemedBy.Int64
	}
	if redeemedAt.Valid {
		value, parseErr := parseTime(redeemedAt.String)
		if parseErr != nil {
			return nil, parseErr
		}
		item.RedeemedAt = &value
	}
	item.CreatedAt, err = parseTime(created)
	return &item, err
}

func (r *Repository) RedeemSupporterCode(ctx context.Context, userID int64, item *domain.SupporterCode, key, requestHash string, now time.Time) error {
	result, err := r.db.ExecContext(ctx, `UPDATE resistance_supporter_codes SET status = 'REDEEMED', redeemed_by = ?, redeemed_at = ? WHERE id = ? AND status = 'AVAILABLE'`, userID, formatTime(now), item.ID)
	if err != nil {
		return err
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrConflict
	}
	profileResult, err := r.db.ExecContext(ctx, `UPDATE resistance_profiles SET gold = gold + ?, updated_at = ? WHERE user_id = ? AND gold <= ?`, item.RewardGold, formatTime(now), userID, domain.MaxGold-item.RewardGold)
	if err != nil {
		return err
	}
	profileCount, _ := profileResult.RowsAffected()
	if profileCount == 0 {
		return r.profileCreditError(ctx, userID)
	}
	if _, err := r.db.ExecContext(ctx, `INSERT INTO resistance_supporter_redemptions (user_id, code_id, idempotency_key, request_hash, reward_gold, created_at) VALUES (?, ?, ?, ?, ?, ?)`, userID, item.ID, key, requestHash, item.RewardGold, formatTime(now)); err != nil {
		return err
	}
	return nil
}

func (r *Repository) profileCreditError(ctx context.Context, userID int64) error {
	var exists int
	if err := r.db.QueryRowContext(ctx, `SELECT EXISTS(SELECT 1 FROM resistance_profiles WHERE user_id = ?)`, userID).Scan(&exists); err != nil {
		return fmt.Errorf("check resistance profile: %w", err)
	}
	if exists == 0 {
		return domain.ErrNotFound
	}
	return domain.ErrConflict
}

func isConstraintError(err error) bool {
	var sqliteErr *modernsqlite.Error
	return errors.As(err, &sqliteErr) && sqliteErr.Code()&0xff == 19
}

func (r *Repository) RedemptionByKey(ctx context.Context, userID int64, key string) (string, int64, error) {
	var hash string
	var reward int64
	err := r.db.QueryRowContext(ctx, `SELECT request_hash, reward_gold FROM resistance_supporter_redemptions WHERE user_id = ? AND idempotency_key = ?`, userID, key).Scan(&hash, &reward)
	if errors.Is(err, sql.ErrNoRows) {
		return "", 0, domain.ErrNotFound
	}
	return hash, reward, err
}

func (r *Repository) ListProfileIDs(ctx context.Context) ([]int64, error) {
	rows, err := r.db.QueryContext(ctx, `SELECT user_id FROM resistance_profiles ORDER BY user_id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var ids []int64
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		ids = append(ids, id)
	}
	return ids, rows.Err()
}

func (r *Repository) PublishNotice(ctx context.Context, item *domain.Notice, now time.Time) error {
	result, err := r.db.ExecContext(ctx, `INSERT INTO resistance_notices (title, body, published_at, created_by_user_id, created_at) VALUES (?, ?, ?, ?, ?)`, item.Title, item.Body, formatTime(now), item.CreatedByUserID, formatTime(now))
	if err != nil {
		return err
	}
	item.ID, _ = result.LastInsertId()
	item.PublishedAt, item.CreatedAt = &now, now
	return nil
}

func (r *Repository) Notices(ctx context.Context, limit int) ([]domain.Notice, error) {
	rows, err := r.db.QueryContext(ctx, `SELECT id, title, body, published_at, created_by_user_id, created_at FROM resistance_notices WHERE published_at IS NOT NULL ORDER BY published_at DESC, id DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]domain.Notice, 0)
	for rows.Next() {
		var item domain.Notice
		var published, created string
		if err := rows.Scan(&item.ID, &item.Title, &item.Body, &published, &item.CreatedByUserID, &created); err != nil {
			return nil, err
		}
		publishedAt, err := parseTime(published)
		if err != nil {
			return nil, err
		}
		item.PublishedAt = &publishedAt
		item.CreatedAt, err = parseTime(created)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func (r *Repository) PublishContent(ctx context.Context, manifest *domain.ContentManifest, adminID int64, now time.Time) error {
	result, err := r.db.ExecContext(ctx, `INSERT INTO resistance_content_versions (version, published_at, created_by_user_id, created_at) VALUES (?, ?, ?, ?)`, manifest.Version, formatTime(now), adminID, formatTime(now))
	if err != nil {
		return err
	}
	versionID, _ := result.LastInsertId()
	for _, entry := range manifest.Entries {
		if _, err := r.db.ExecContext(ctx, `INSERT INTO resistance_content_entries (version_id, chapter_id, content_id, media_type, body, sha256_hex, ordinal) VALUES (?, ?, ?, ?, ?, ?, ?)`, versionID, entry.ChapterID, entry.ID, entry.MediaType, entry.Body, entry.SHA256, entry.Ordinal); err != nil {
			return err
		}
	}
	manifest.PublishedAt = now
	return nil
}

func (r *Repository) LatestContent(ctx context.Context) (*domain.ContentManifest, error) {
	var id int64
	var item domain.ContentManifest
	var published string
	err := r.db.QueryRowContext(ctx, `SELECT id, version, published_at FROM resistance_content_versions WHERE published_at IS NOT NULL ORDER BY published_at DESC, id DESC LIMIT 1`).Scan(&id, &item.Version, &published)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	item.PublishedAt, err = parseTime(published)
	if err != nil {
		return nil, err
	}
	rows, err := r.db.QueryContext(ctx, `SELECT chapter_id, content_id, media_type, body, sha256_hex, ordinal FROM resistance_content_entries WHERE version_id = ? ORDER BY ordinal, content_id`, id)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var entry domain.ContentEntry
		if err := rows.Scan(&entry.ChapterID, &entry.ID, &entry.MediaType, &entry.Body, &entry.SHA256, &entry.Ordinal); err != nil {
			return nil, err
		}
		item.Entries = append(item.Entries, entry)
	}
	return &item, rows.Err()
}

func (r *Repository) LatestContentItem(ctx context.Context, contentID string) (*domain.ContentEntry, error) {
	var item domain.ContentEntry
	err := r.db.QueryRowContext(ctx, `SELECT entry.chapter_id, entry.content_id, entry.media_type, entry.body, entry.sha256_hex, entry.ordinal FROM resistance_content_entries entry JOIN resistance_content_versions version ON version.id = entry.version_id WHERE version.published_at IS NOT NULL AND entry.content_id = ? ORDER BY version.published_at DESC, version.id DESC LIMIT 1`, contentID).
		Scan(&item.ChapterID, &item.ID, &item.MediaType, &item.Body, &item.SHA256, &item.Ordinal)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	return &item, nil
}
