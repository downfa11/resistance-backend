package users

import (
	"context"
	"errors"
	"path/filepath"
	"testing"
	"time"

	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
)

func TestRepositoryCreatesAndFindsUserCaseInsensitively(t *testing.T) {
	t.Parallel()

	repository := newTestRepository(t)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	created := &User{
		Account:      "Player.One",
		Email:        "Player@Example.com",
		PasswordHash: "encoded-password",
		DisplayName:  "Player One",
		Role:         RolePlayer,
		Status:       StatusActive,
		CreatedAt:    now,
		UpdatedAt:    now,
	}
	if err := repository.Create(context.Background(), created); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID < 1 {
		t.Fatalf("created ID = %d", created.ID)
	}

	byAccount, err := repository.FindByAccount(context.Background(), "player.one")
	if err != nil {
		t.Fatalf("FindByAccount() error = %v", err)
	}
	if byAccount.ID != created.ID || byAccount.Email != created.Email {
		t.Fatalf("FindByAccount() = %#v", byAccount)
	}

	byEmail, err := repository.FindByEmail(context.Background(), "PLAYER@example.COM")
	if err != nil {
		t.Fatalf("FindByEmail() error = %v", err)
	}
	if byEmail.ID != created.ID {
		t.Fatalf("FindByEmail().ID = %d, want %d", byEmail.ID, created.ID)
	}
}

func TestRepositoryReturnsTypedConflictAndNotFound(t *testing.T) {
	t.Parallel()

	repository := newTestRepository(t)
	now := time.Now().UTC()
	first := &User{Account: "player", Email: "one@example.com", PasswordHash: "hash", DisplayName: "One", Role: RolePlayer, Status: StatusActive, CreatedAt: now, UpdatedAt: now}
	if err := repository.Create(context.Background(), first); err != nil {
		t.Fatalf("first Create() error = %v", err)
	}
	duplicate := &User{Account: "PLAYER", Email: "two@example.com", PasswordHash: "hash", DisplayName: "Two", Role: RolePlayer, Status: StatusActive, CreatedAt: now, UpdatedAt: now}
	if err := repository.Create(context.Background(), duplicate); !errors.Is(err, ErrConflict) {
		t.Fatalf("duplicate Create() error = %v, want ErrConflict", err)
	}
	if _, err := repository.FindByID(context.Background(), 404); !errors.Is(err, ErrNotFound) {
		t.Fatalf("FindByID() error = %v, want ErrNotFound", err)
	}
}

func newTestRepository(t *testing.T) *Repository {
	t.Helper()
	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	return NewRepository(db)
}
