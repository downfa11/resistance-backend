package auth

import (
	"context"
	"crypto/sha256"
	"errors"
	"path/filepath"
	"testing"
	"time"

	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestSessionRepositoryRotatesAndRevokesReusedFamily(t *testing.T) {
	t.Parallel()

	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	userRepository := users.NewRepository(db)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	user := &users.User{Account: "player", Email: "player@example.com", PasswordHash: "hash", DisplayName: "Player", Role: users.RolePlayer, Status: users.StatusActive, CreatedAt: now, UpdatedAt: now}
	if err := userRepository.Create(context.Background(), user); err != nil {
		t.Fatalf("create user: %v", err)
	}

	repository := NewSessionRepository(db)
	firstHash := sha256.Sum256([]byte("first-refresh-token"))
	first := &RefreshSession{ID: "session-1", FamilyID: "session-1", UserID: user.ID, TokenHash: firstHash[:], ExpiresAt: now.Add(24 * time.Hour), CreatedAt: now}
	if err := repository.Create(context.Background(), first); err != nil {
		t.Fatalf("Create() error = %v", err)
	}

	secondHash := sha256.Sum256([]byte("second-refresh-token"))
	second := &RefreshSession{ID: "session-2", TokenHash: secondHash[:], ExpiresAt: now.Add(24 * time.Hour), CreatedAt: now.Add(time.Minute)}
	rotated, err := repository.Rotate(context.Background(), firstHash[:], second, now.Add(time.Minute))
	if err != nil {
		t.Fatalf("Rotate() error = %v", err)
	}
	if rotated.UserID != user.ID || rotated.FamilyID != first.FamilyID || rotated.ParentSessionID != first.ID {
		t.Fatalf("rotated = %#v", rotated)
	}

	thirdHash := sha256.Sum256([]byte("third-refresh-token"))
	_, err = repository.Rotate(context.Background(), firstHash[:], &RefreshSession{ID: "session-3", TokenHash: thirdHash[:], ExpiresAt: now.Add(24 * time.Hour), CreatedAt: now.Add(2 * time.Minute)}, now.Add(2*time.Minute))
	if !errors.Is(err, ErrSessionReused) {
		t.Fatalf("Rotate(reused) error = %v, want ErrSessionReused", err)
	}
	secondAfterReuse, err := repository.FindByTokenHash(context.Background(), secondHash[:])
	if err != nil {
		t.Fatalf("FindByTokenHash() error = %v", err)
	}
	if secondAfterReuse.RevokedAt == nil {
		t.Fatal("replacement session was not revoked after token reuse")
	}
}

func TestSessionRepositoryRejectsExpiredAndSupportsLogout(t *testing.T) {
	t.Parallel()

	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	userRepository := users.NewRepository(db)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	user := &users.User{Account: "player", Email: "player@example.com", PasswordHash: "hash", DisplayName: "Player", Role: users.RolePlayer, Status: users.StatusActive, CreatedAt: now, UpdatedAt: now}
	if err := userRepository.Create(context.Background(), user); err != nil {
		t.Fatalf("create user: %v", err)
	}

	repository := NewSessionRepository(db)
	tokenHash := sha256.Sum256([]byte("refresh-token"))
	session := &RefreshSession{ID: "session-1", FamilyID: "session-1", UserID: user.ID, TokenHash: tokenHash[:], ExpiresAt: now.Add(-time.Minute), CreatedAt: now.Add(-time.Hour)}
	if err := repository.Create(context.Background(), session); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	_, err = repository.Rotate(context.Background(), tokenHash[:], &RefreshSession{ID: "session-2", TokenHash: []byte("replacement"), ExpiresAt: now.Add(time.Hour), CreatedAt: now}, now)
	if !errors.Is(err, ErrSessionExpired) {
		t.Fatalf("Rotate(expired) error = %v, want ErrSessionExpired", err)
	}
	if err := repository.RevokeByTokenHash(context.Background(), tokenHash[:], now); err != nil {
		t.Fatalf("RevokeByTokenHash() error = %v", err)
	}
	found, err := repository.FindByTokenHash(context.Background(), tokenHash[:])
	if err != nil {
		t.Fatalf("FindByTokenHash() error = %v", err)
	}
	if found.RevokedAt == nil {
		t.Fatal("session RevokedAt = nil")
	}
	deleted, err := repository.DeleteExpiredFamilies(context.Background(), now, 1)
	if err != nil {
		t.Fatalf("DeleteExpiredFamilies() error = %v", err)
	}
	if deleted != 1 {
		t.Fatalf("DeleteExpiredFamilies() = %d, want 1", deleted)
	}
}
