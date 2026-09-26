package service

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"

	"github.com/downfa11/resistance-backend/internal/platform/notifications"
	"github.com/downfa11/resistance-backend/internal/resistance/domain"
	"github.com/downfa11/resistance-backend/internal/resistance/repository"
)

func (s *Service) Rates(ctx context.Context) ([]domain.CurrencyRate, error) { return s.repo.Rates(ctx) }

func (s *Service) AdjustRates(ctx context.Context) ([]domain.CurrencyRate, error) {
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	items, err := repository.New(tx).AdjustRates(ctx, s.now().UTC())
	if err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return items, nil
}

func (s *Service) ResetRates(ctx context.Context) ([]domain.CurrencyRate, error) {
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	items, err := repository.New(tx).ResetRates(ctx, s.now().UTC())
	if err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return items, nil
}
func (s *Service) Balances(ctx context.Context, userID int64) ([]domain.CurrencyBalance, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.Balances(ctx, userID)
}

func (s *Service) SetBalance(ctx context.Context, userID int64, code string, quantity int64) error {
	code = strings.ToUpper(strings.TrimSpace(code))
	if userID < 1 || code == "" || quantity < 0 || quantity > 1000000 {
		return domain.ErrInvalidInput
	}
	return s.repo.SetBalance(ctx, userID, code, quantity, s.now().UTC())
}

func (s *Service) Exchange(ctx context.Context, userID int64, code string, quantity int64, key string) (*domain.Exchange, error) {
	code, key = strings.ToUpper(strings.TrimSpace(code)), strings.TrimSpace(key)
	if userID < 1 || code == "" || quantity < 1 || key == "" || len(key) > 128 {
		return nil, domain.ErrInvalidInput
	}
	hash := requestHash(code, fmt.Sprint(quantity))
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	repo := repository.New(tx)
	existing, err := repo.FindExchangeByKey(ctx, userID, key)
	if err == nil {
		if existing.RequestHash != hash {
			return nil, domain.ErrIdempotencyConflict
		}
		return &existing.Exchange, nil
	}
	if !errors.Is(err, domain.ErrNotFound) {
		return nil, err
	}
	item, err := repo.Exchange(ctx, userID, code, quantity, key, hash, now)
	if err != nil {
		return nil, err
	}
	if _, err := notifications.NewService(notifications.NewRepository(tx)).Create(ctx, notifications.CreateRequest{UserID: userID, Source: notifications.SourceResistance, Title: "Exchange complete", Body: fmt.Sprintf("Exchanged %d %s for %d Gold.", quantity, code, item.GoldGranted), DeepLink: "resistance://wallet"}); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return item, nil
}

func (s *Service) CreateSupporterCode(ctx context.Context, kind, code string, rewardGold int64) (*domain.SupporterCode, error) {
	kind, code = strings.TrimSpace(kind), strings.ToUpper(strings.TrimSpace(code))
	if kind == "" {
		kind = "custom"
	}
	if code == "" || len(code) > 64 || rewardGold < 0 || rewardGold > domain.MaxGold {
		return nil, domain.ErrInvalidInput
	}
	item := &domain.SupporterCode{Kind: kind, Code: code, RewardGold: rewardGold, Status: "AVAILABLE", CreatedAt: s.now().UTC()}
	if err := s.repo.CreateSupporterCode(ctx, item); err != nil {
		return nil, err
	}
	return item, nil
}

func (s *Service) SupporterCodes(ctx context.Context, limit int) ([]domain.SupporterCode, error) {
	return s.repo.SupporterCodes(ctx, limit)
}

func (s *Service) DeleteSupporterCode(ctx context.Context, id int64) error {
	if id < 1 {
		return domain.ErrInvalidInput
	}
	return s.repo.DeleteSupporterCode(ctx, id)
}

func (s *Service) SupporterDetails(ctx context.Context) ([]domain.SupporterDetail, error) {
	return s.repo.SupporterDetails(ctx)
}
func (s *Service) CreateSupporterDetail(ctx context.Context, title, details string) (*domain.SupporterDetail, error) {
	title, details = strings.TrimSpace(title), strings.TrimSpace(details)
	if title == "" || details == "" {
		return nil, domain.ErrInvalidInput
	}
	now := s.now().UTC()
	item := &domain.SupporterDetail{Title: title, Details: details, CreatedAt: now, UpdatedAt: now}
	if err := s.repo.CreateSupporterDetail(ctx, item); err != nil {
		return nil, err
	}
	return item, nil
}
func (s *Service) UpdateSupporterDetail(ctx context.Context, id int64, title, details string) (*domain.SupporterDetail, error) {
	title, details = strings.TrimSpace(title), strings.TrimSpace(details)
	if id < 1 || title == "" || details == "" {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.UpdateSupporterDetail(ctx, domain.SupporterDetail{ID: id, Title: title, Details: details, UpdatedAt: s.now().UTC()})
}
func (s *Service) DeleteSupporterDetail(ctx context.Context, id int64) error {
	if id < 1 {
		return domain.ErrInvalidInput
	}
	return s.repo.DeleteSupporterDetail(ctx, id)
}

type Redemption struct {
	Code       string
	RewardGold int64
}

func (s *Service) RedeemSupporterCode(ctx context.Context, userID int64, code, key string) (*Redemption, error) {
	code, key = strings.ToUpper(strings.TrimSpace(code)), strings.TrimSpace(key)
	if userID < 1 || code == "" || key == "" || len(key) > 128 {
		return nil, domain.ErrInvalidInput
	}
	hash := requestHash(code)
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	repo := repository.New(tx)
	existingHash, reward, err := repo.RedemptionByKey(ctx, userID, key)
	if err == nil {
		if existingHash != hash {
			return nil, domain.ErrIdempotencyConflict
		}
		return &Redemption{Code: code, RewardGold: reward}, nil
	}
	if !errors.Is(err, domain.ErrNotFound) {
		return nil, err
	}
	item, err := repo.SupporterCode(ctx, code)
	if err != nil {
		return nil, err
	}
	if err := repo.RedeemSupporterCode(ctx, userID, item, key, hash, now); err != nil {
		return nil, err
	}
	if _, err := notifications.NewService(notifications.NewRepository(tx)).Create(ctx, notifications.CreateRequest{UserID: userID, Source: notifications.SourceResistance, Title: "Supporter reward", Body: fmt.Sprintf("Supporter code redeemed for %d Gold.", item.RewardGold), DeepLink: "resistance://wallet"}); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return &Redemption{Code: code, RewardGold: item.RewardGold}, nil
}

func (s *Service) PublishNotice(ctx context.Context, adminID int64, title, body string) (*domain.Notice, error) {
	title, body = strings.TrimSpace(title), strings.TrimSpace(body)
	if adminID < 1 || title == "" || body == "" || len([]rune(title)) > 160 || len([]rune(body)) > 4000 {
		return nil, domain.ErrInvalidInput
	}
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	repo := repository.New(tx)
	item := &domain.Notice{Title: title, Body: body, CreatedByUserID: adminID}
	if err := repo.PublishNotice(ctx, item, now); err != nil {
		return nil, err
	}
	ids, err := repo.ListProfileIDs(ctx)
	if err != nil {
		return nil, err
	}
	writer := notifications.NewService(notifications.NewRepository(tx))
	for _, id := range ids {
		if _, err := writer.Create(ctx, notifications.CreateRequest{UserID: id, Source: notifications.SourceResistance, Title: title, Body: body, DeepLink: fmt.Sprintf("resistance://notices/%d", item.ID)}); err != nil {
			return nil, err
		}
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return item, nil
}

func (s *Service) Notices(ctx context.Context) ([]domain.Notice, error) {
	return s.repo.Notices(ctx, 100)
}

func (s *Service) PublishContent(ctx context.Context, adminID int64, version string, entries []domain.ContentEntry) (*domain.ContentManifest, error) {
	version = strings.TrimSpace(version)
	if adminID < 1 || version == "" || len(entries) == 0 {
		return nil, domain.ErrInvalidInput
	}
	seen := make(map[string]struct{}, len(entries))
	for index := range entries {
		entries[index].ChapterID = strings.TrimSpace(entries[index].ChapterID)
		entries[index].ID = strings.TrimSpace(entries[index].ID)
		entries[index].MediaType = strings.TrimSpace(entries[index].MediaType)
		if entries[index].ChapterID == "" || entries[index].ID == "" || entries[index].MediaType == "" {
			return nil, domain.ErrInvalidInput
		}
		if _, exists := seen[entries[index].ID]; exists {
			return nil, domain.ErrInvalidInput
		}
		seen[entries[index].ID] = struct{}{}
		sum := sha256.Sum256([]byte(entries[index].Body))
		entries[index].SHA256 = hex.EncodeToString(sum[:])
		entries[index].Ordinal = index
	}
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	repo := repository.New(tx)
	manifest := &domain.ContentManifest{Version: version, Entries: entries}
	if err := repo.PublishContent(ctx, manifest, adminID, now); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return manifest, nil
}

func (s *Service) Content(ctx context.Context) (*domain.ContentManifest, error) {
	return s.repo.LatestContent(ctx)
}

func (s *Service) ContentItem(ctx context.Context, id string) (*domain.ContentEntry, error) {
	id = strings.TrimSpace(id)
	if id == "" {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.LatestContentItem(ctx, id)
}

func requestHash(parts ...string) string {
	sum := sha256.Sum256([]byte(strings.Join(parts, "\x00")))
	return hex.EncodeToString(sum[:])
}
