package auth

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	modernsqlite "modernc.org/sqlite"
)

var (
	ErrSessionNotFound = errors.New("refresh session not found")
	ErrSessionExpired  = errors.New("refresh session expired")
	ErrSessionRevoked  = errors.New("refresh session revoked")
	ErrSessionReused   = errors.New("rotated refresh session reused")
	ErrSessionConflict = errors.New("refresh session conflict")
)

const sessionColumns = `id, family_id, user_id, token_hash, parent_session_id, replaced_by_session_id, expires_at, revoked_at, created_at`

type RefreshSession struct {
	ID                  string
	FamilyID            string
	UserID              int64
	TokenHash           []byte
	ParentSessionID     string
	ReplacedBySessionID string
	ExpiresAt           time.Time
	RevokedAt           *time.Time
	CreatedAt           time.Time
}

type SessionRepository struct {
	db *sql.DB
}

func NewSessionRepository(db *sql.DB) *SessionRepository {
	return &SessionRepository{db: db}
}

func (r *SessionRepository) Create(ctx context.Context, session *RefreshSession) error {
	if r == nil || r.db == nil || session == nil {
		return fmt.Errorf("create refresh session: repository and session are required")
	}
	_, err := r.db.ExecContext(ctx, `
		INSERT INTO refresh_sessions (id, family_id, user_id, token_hash, parent_session_id, replaced_by_session_id, expires_at, revoked_at, created_at)
		VALUES (?, ?, ?, ?, NULL, NULL, ?, NULL, ?)
	`, session.ID, session.FamilyID, session.UserID, session.TokenHash, formatSessionTime(session.ExpiresAt), formatSessionTime(session.CreatedAt))
	if err != nil {
		if isSessionConstraintError(err) {
			return fmt.Errorf("%w: %v", ErrSessionConflict, err)
		}
		return fmt.Errorf("insert refresh session: %w", err)
	}
	return nil
}

func (r *SessionRepository) FindByTokenHash(ctx context.Context, tokenHash []byte) (*RefreshSession, error) {
	if r == nil || r.db == nil {
		return nil, fmt.Errorf("find refresh session: repository is required")
	}
	return findSession(r.db.QueryRowContext(ctx, `SELECT `+sessionColumns+` FROM refresh_sessions WHERE token_hash = ?`, tokenHash))
}

func (r *SessionRepository) Rotate(ctx context.Context, tokenHash []byte, replacement *RefreshSession, now time.Time) (*RefreshSession, error) {
	if r == nil || r.db == nil || replacement == nil {
		return nil, fmt.Errorf("rotate refresh session: repository and replacement are required")
	}
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("begin refresh rotation: %w", err)
	}
	defer func() { _ = tx.Rollback() }()

	current, err := findSession(tx.QueryRowContext(ctx, `SELECT `+sessionColumns+` FROM refresh_sessions WHERE token_hash = ?`, tokenHash))
	if err != nil {
		return nil, err
	}
	if current.ReplacedBySessionID != "" {
		if err := revokeFamily(ctx, tx, current.FamilyID, now); err != nil {
			return nil, err
		}
		if err := tx.Commit(); err != nil {
			return nil, fmt.Errorf("commit reused session revocation: %w", err)
		}
		return nil, ErrSessionReused
	}
	if current.RevokedAt != nil {
		return nil, ErrSessionRevoked
	}
	if !current.ExpiresAt.After(now) {
		return nil, ErrSessionExpired
	}

	replacement.FamilyID = current.FamilyID
	replacement.UserID = current.UserID
	replacement.ParentSessionID = current.ID
	_, err = tx.ExecContext(ctx, `
		INSERT INTO refresh_sessions (id, family_id, user_id, token_hash, parent_session_id, replaced_by_session_id, expires_at, revoked_at, created_at)
		VALUES (?, ?, ?, ?, ?, NULL, ?, NULL, ?)
	`, replacement.ID, replacement.FamilyID, replacement.UserID, replacement.TokenHash, replacement.ParentSessionID, formatSessionTime(replacement.ExpiresAt), formatSessionTime(replacement.CreatedAt))
	if err != nil {
		if isSessionConstraintError(err) {
			return nil, fmt.Errorf("%w: %v", ErrSessionConflict, err)
		}
		return nil, fmt.Errorf("insert rotated refresh session: %w", err)
	}
	result, err := tx.ExecContext(ctx, `
		UPDATE refresh_sessions
		SET replaced_by_session_id = ?
		WHERE id = ? AND replaced_by_session_id IS NULL AND revoked_at IS NULL
	`, replacement.ID, current.ID)
	if err != nil {
		return nil, fmt.Errorf("replace refresh session: %w", err)
	}
	rows, err := result.RowsAffected()
	if err != nil || rows != 1 {
		return nil, ErrSessionConflict
	}
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("commit refresh rotation: %w", err)
	}
	return replacement, nil
}

func (r *SessionRepository) RevokeByTokenHash(ctx context.Context, tokenHash []byte, now time.Time) error {
	if r == nil || r.db == nil {
		return fmt.Errorf("revoke refresh session: repository is required")
	}
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin refresh revocation: %w", err)
	}
	defer func() { _ = tx.Rollback() }()
	session, err := findSession(tx.QueryRowContext(ctx, `SELECT `+sessionColumns+` FROM refresh_sessions WHERE token_hash = ?`, tokenHash))
	if err != nil {
		return err
	}
	if err := revokeFamily(ctx, tx, session.FamilyID, now); err != nil {
		return err
	}
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("commit refresh revocation: %w", err)
	}
	return nil
}

func (r *SessionRepository) DeleteExpiredFamilies(ctx context.Context, now time.Time, familyLimit int) (int64, error) {
	if r == nil || r.db == nil || familyLimit < 1 {
		return 0, fmt.Errorf("delete expired refresh sessions: repository and positive limit are required")
	}
	result, err := r.db.ExecContext(ctx, `
		DELETE FROM refresh_sessions
		WHERE family_id IN (
			SELECT family_id
			FROM refresh_sessions
			GROUP BY family_id
			HAVING MAX(expires_at) <= ?
			ORDER BY MIN(created_at)
			LIMIT ?
		)
	`, formatSessionTime(now), familyLimit)
	if err != nil {
		return 0, fmt.Errorf("delete expired refresh sessions: %w", err)
	}
	count, err := result.RowsAffected()
	if err != nil {
		return 0, fmt.Errorf("count deleted refresh sessions: %w", err)
	}
	return count, nil
}

type rowScanner interface {
	Scan(...any) error
}

func findSession(row rowScanner) (*RefreshSession, error) {
	var (
		session                              RefreshSession
		parentID, replacementID, revokedText sql.NullString
		expiresText, createdText             string
	)
	err := row.Scan(
		&session.ID,
		&session.FamilyID,
		&session.UserID,
		&session.TokenHash,
		&parentID,
		&replacementID,
		&expiresText,
		&revokedText,
		&createdText,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrSessionNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("query refresh session: %w", err)
	}
	session.ParentSessionID = parentID.String
	session.ReplacedBySessionID = replacementID.String
	session.ExpiresAt, err = time.Parse(time.RFC3339Nano, expiresText)
	if err != nil {
		return nil, fmt.Errorf("parse refresh expiry: %w", err)
	}
	session.CreatedAt, err = time.Parse(time.RFC3339Nano, createdText)
	if err != nil {
		return nil, fmt.Errorf("parse refresh created_at: %w", err)
	}
	if revokedText.Valid {
		revokedAt, err := time.Parse(time.RFC3339Nano, revokedText.String)
		if err != nil {
			return nil, fmt.Errorf("parse refresh revoked_at: %w", err)
		}
		session.RevokedAt = &revokedAt
	}
	return &session, nil
}

func revokeFamily(ctx context.Context, tx *sql.Tx, familyID string, now time.Time) error {
	if _, err := tx.ExecContext(ctx, `
		UPDATE refresh_sessions
		SET revoked_at = COALESCE(revoked_at, ?)
		WHERE family_id = ?
	`, formatSessionTime(now), familyID); err != nil {
		return fmt.Errorf("revoke refresh session family: %w", err)
	}
	return nil
}

func formatSessionTime(value time.Time) string {
	return value.UTC().Format(time.RFC3339Nano)
}

func isSessionConstraintError(err error) bool {
	var sqliteErr *modernsqlite.Error
	return errors.As(err, &sqliteErr) && sqliteErr.Code()&0xff == 19
}
