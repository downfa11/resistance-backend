package users

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	modernsqlite "modernc.org/sqlite"
)

const userColumns = `id, account, email, password_hash, display_name, role, status, created_at, updated_at`

type Repository struct {
	db *sql.DB
}

func NewRepository(db *sql.DB) *Repository {
	return &Repository{db: db}
}

func (r *Repository) Create(ctx context.Context, user *User) error {
	if r == nil || r.db == nil || user == nil {
		return fmt.Errorf("create user: repository and user are required")
	}
	result, err := r.db.ExecContext(ctx, `
		INSERT INTO users (account, email, password_hash, display_name, role, status, created_at, updated_at)
		VALUES (?, ?, ?, ?, ?, ?, ?, ?)
	`, user.Account, user.Email, user.PasswordHash, user.DisplayName, user.Role, user.Status, formatTime(user.CreatedAt), formatTime(user.UpdatedAt))
	if err != nil {
		if isConstraintError(err) {
			return fmt.Errorf("%w: %v", ErrConflict, err)
		}
		return fmt.Errorf("insert user: %w", err)
	}
	id, err := result.LastInsertId()
	if err != nil {
		return fmt.Errorf("read inserted user id: %w", err)
	}
	user.ID = id
	return nil
}

func (r *Repository) FindByID(ctx context.Context, id int64) (*User, error) {
	return r.find(ctx, `SELECT `+userColumns+` FROM users WHERE id = ?`, id)
}

func (r *Repository) FindByAccount(ctx context.Context, account string) (*User, error) {
	return r.find(ctx, `SELECT `+userColumns+` FROM users WHERE account = ? COLLATE NOCASE`, account)
}

func (r *Repository) FindByEmail(ctx context.Context, email string) (*User, error) {
	return r.find(ctx, `SELECT `+userColumns+` FROM users WHERE email = ? COLLATE NOCASE`, email)
}

func (r *Repository) find(ctx context.Context, query string, argument any) (*User, error) {
	if r == nil || r.db == nil {
		return nil, fmt.Errorf("find user: repository is required")
	}
	var (
		user                         User
		createdAtText, updatedAtText string
	)
	err := r.db.QueryRowContext(ctx, query, argument).Scan(
		&user.ID,
		&user.Account,
		&user.Email,
		&user.PasswordHash,
		&user.DisplayName,
		&user.Role,
		&user.Status,
		&createdAtText,
		&updatedAtText,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("query user: %w", err)
	}
	user.CreatedAt, err = time.Parse(time.RFC3339Nano, createdAtText)
	if err != nil {
		return nil, fmt.Errorf("parse user created_at: %w", err)
	}
	user.UpdatedAt, err = time.Parse(time.RFC3339Nano, updatedAtText)
	if err != nil {
		return nil, fmt.Errorf("parse user updated_at: %w", err)
	}
	return &user, nil
}

func formatTime(value time.Time) string {
	return value.UTC().Format(time.RFC3339Nano)
}

func isConstraintError(err error) bool {
	var sqliteErr *modernsqlite.Error
	return errors.As(err, &sqliteErr) && sqliteErr.Code()&0xff == 19
}
