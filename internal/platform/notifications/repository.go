package notifications

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"
)

const notificationColumns = `id, user_id, source, title, body, deep_link, read_at, expires_at, created_at`

type DBTX interface {
	ExecContext(context.Context, string, ...any) (sql.Result, error)
	QueryContext(context.Context, string, ...any) (*sql.Rows, error)
	QueryRowContext(context.Context, string, ...any) *sql.Row
}

type Repository struct {
	db DBTX
}

func NewRepository(db DBTX) *Repository {
	return &Repository{db: db}
}

func (r *Repository) Create(ctx context.Context, item *Notification) error {
	if r == nil || r.db == nil || item == nil {
		return fmt.Errorf("create notification: repository and notification are required")
	}
	result, err := r.db.ExecContext(ctx, `
		INSERT INTO notifications (user_id, source, title, body, deep_link, read_at, expires_at, created_at)
		VALUES (?, ?, ?, ?, ?, NULL, ?, ?)
	`, item.UserID, item.Source, item.Title, item.Body, nullableString(item.DeepLink), nullableTime(item.ExpiresAt), formatTime(item.CreatedAt))
	if err != nil {
		return fmt.Errorf("insert notification: %w", err)
	}
	item.ID, err = result.LastInsertId()
	if err != nil {
		return fmt.Errorf("read inserted notification id: %w", err)
	}
	return nil
}

func (r *Repository) List(ctx context.Context, userID int64, limit int, beforeID int64, now time.Time) ([]*Notification, error) {
	rows, err := r.db.QueryContext(ctx, `
		SELECT `+notificationColumns+`
		FROM notifications
		WHERE user_id = ?
		  AND (expires_at IS NULL OR expires_at > ?)
		  AND (? = 0 OR id < ?)
		ORDER BY id DESC
		LIMIT ?
	`, userID, formatTime(now), beforeID, beforeID, limit)
	if err != nil {
		return nil, fmt.Errorf("list notifications: %w", err)
	}
	defer rows.Close()

	items := make([]*Notification, 0)
	for rows.Next() {
		item, err := scanNotification(rows)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate notifications: %w", err)
	}
	return items, nil
}

func (r *Repository) Find(ctx context.Context, userID, id int64, now time.Time) (*Notification, error) {
	return scanNotification(r.db.QueryRowContext(ctx, `
		SELECT `+notificationColumns+`
		FROM notifications
		WHERE id = ? AND user_id = ? AND (expires_at IS NULL OR expires_at > ?)
	`, id, userID, formatTime(now)))
}

func (r *Repository) MarkRead(ctx context.Context, userID, id int64, now time.Time) (*Notification, error) {
	result, err := r.db.ExecContext(ctx, `
		UPDATE notifications
		SET read_at = COALESCE(read_at, ?)
		WHERE id = ? AND user_id = ? AND (expires_at IS NULL OR expires_at > ?)
	`, formatTime(now), id, userID, formatTime(now))
	if err != nil {
		return nil, fmt.Errorf("mark notification read: %w", err)
	}
	count, err := result.RowsAffected()
	if err != nil {
		return nil, fmt.Errorf("count marked notification: %w", err)
	}
	if count == 0 {
		return nil, ErrNotFound
	}
	return r.Find(ctx, userID, id, now)
}

func (r *Repository) MarkAllRead(ctx context.Context, userID int64, now time.Time) (int64, error) {
	result, err := r.db.ExecContext(ctx, `
		UPDATE notifications
		SET read_at = ?
		WHERE user_id = ? AND read_at IS NULL AND (expires_at IS NULL OR expires_at > ?)
	`, formatTime(now), userID, formatTime(now))
	if err != nil {
		return 0, fmt.Errorf("mark all notifications read: %w", err)
	}
	count, err := result.RowsAffected()
	if err != nil {
		return 0, fmt.Errorf("count marked notifications: %w", err)
	}
	return count, nil
}

func (r *Repository) DeleteExpired(ctx context.Context, now time.Time, limit int) (int64, error) {
	if limit < 1 {
		return 0, ErrInvalidInput
	}
	result, err := r.db.ExecContext(ctx, `
		DELETE FROM notifications
		WHERE id IN (
			SELECT id FROM notifications
			WHERE expires_at IS NOT NULL AND expires_at <= ?
			ORDER BY id
			LIMIT ?
		)
	`, formatTime(now), limit)
	if err != nil {
		return 0, fmt.Errorf("delete expired notifications: %w", err)
	}
	count, err := result.RowsAffected()
	if err != nil {
		return 0, fmt.Errorf("count deleted notifications: %w", err)
	}
	return count, nil
}

type rowScanner interface {
	Scan(...any) error
}

func scanNotification(row rowScanner) (*Notification, error) {
	var (
		item                            Notification
		deepLink, readText, expiresText sql.NullString
		createdText                     string
	)
	err := row.Scan(&item.ID, &item.UserID, &item.Source, &item.Title, &item.Body, &deepLink, &readText, &expiresText, &createdText)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("scan notification: %w", err)
	}
	item.DeepLink = deepLink.String
	item.CreatedAt, err = time.Parse(time.RFC3339Nano, createdText)
	if err != nil {
		return nil, fmt.Errorf("parse notification created_at: %w", err)
	}
	if readText.Valid {
		readAt, err := time.Parse(time.RFC3339Nano, readText.String)
		if err != nil {
			return nil, fmt.Errorf("parse notification read_at: %w", err)
		}
		item.ReadAt = &readAt
	}
	if expiresText.Valid {
		expiresAt, err := time.Parse(time.RFC3339Nano, expiresText.String)
		if err != nil {
			return nil, fmt.Errorf("parse notification expires_at: %w", err)
		}
		item.ExpiresAt = &expiresAt
	}
	return &item, nil
}

func nullableString(value string) any {
	if value == "" {
		return nil
	}
	return value
}

func nullableTime(value *time.Time) any {
	if value == nil {
		return nil
	}
	return formatTime(*value)
}

func formatTime(value time.Time) string {
	return value.UTC().Format(time.RFC3339Nano)
}
