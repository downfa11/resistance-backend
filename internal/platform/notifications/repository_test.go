package notifications

import (
	"context"
	"errors"
	"path/filepath"
	"testing"
	"time"

	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestRepositoryScopesInboxAndReadStateToRecipient(t *testing.T) {
	t.Parallel()

	repository, firstUserID, secondUserID := newTestRepository(t)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	first := &Notification{UserID: firstUserID, Source: SourceResistance, Title: "Friend request", Body: "A friend request arrived.", DeepLink: "/friends", CreatedAt: now}
	second := &Notification{UserID: secondUserID, Source: SourceResistance, Title: "Friend request", Body: "A friend request arrived.", CreatedAt: now.Add(time.Minute)}
	expired := &Notification{UserID: firstUserID, Source: SourceSystem, Title: "Expired", Body: "Old message.", ExpiresAt: timePointer(now.Add(-time.Minute)), CreatedAt: now.Add(-time.Hour)}
	for _, item := range []*Notification{first, second, expired} {
		if err := repository.Create(context.Background(), item); err != nil {
			t.Fatalf("Create(%s) error = %v", item.Title, err)
		}
	}

	items, err := repository.List(context.Background(), firstUserID, 20, 0, now)
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if len(items) != 1 || items[0].ID != first.ID {
		t.Fatalf("List() = %#v", items)
	}
	if _, err := repository.Find(context.Background(), secondUserID, first.ID, now); !errors.Is(err, ErrNotFound) {
		t.Fatalf("cross-user Find() error = %v, want ErrNotFound", err)
	}

	read, err := repository.MarkRead(context.Background(), firstUserID, first.ID, now.Add(2*time.Minute))
	if err != nil {
		t.Fatalf("MarkRead() error = %v", err)
	}
	if read.ReadAt == nil {
		t.Fatal("ReadAt = nil")
	}
}

func TestRepositoryUsesCursorAndMarksAllRead(t *testing.T) {
	t.Parallel()

	repository, userID, _ := newTestRepository(t)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	for index := 0; index < 3; index++ {
		item := &Notification{UserID: userID, Source: SourceSystem, Title: "Message", Body: "Body", CreatedAt: now.Add(time.Duration(index) * time.Minute)}
		if err := repository.Create(context.Background(), item); err != nil {
			t.Fatalf("Create() error = %v", err)
		}
	}
	firstPage, err := repository.List(context.Background(), userID, 2, 0, now.Add(time.Hour))
	if err != nil {
		t.Fatalf("List(first) error = %v", err)
	}
	if len(firstPage) != 2 || firstPage[0].ID <= firstPage[1].ID {
		t.Fatalf("first page = %#v", firstPage)
	}
	secondPage, err := repository.List(context.Background(), userID, 2, firstPage[1].ID, now.Add(time.Hour))
	if err != nil {
		t.Fatalf("List(second) error = %v", err)
	}
	if len(secondPage) != 1 {
		t.Fatalf("second page length = %d", len(secondPage))
	}
	count, err := repository.MarkAllRead(context.Background(), userID, now.Add(2*time.Hour))
	if err != nil {
		t.Fatalf("MarkAllRead() error = %v", err)
	}
	if count != 3 {
		t.Fatalf("MarkAllRead() count = %d, want 3", count)
	}
}

func TestRepositoryDeletesExpiredNotificationsInBoundedBatches(t *testing.T) {
	t.Parallel()

	repository, userID, _ := newTestRepository(t)
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	for index := 0; index < 2; index++ {
		expiresAt := now.Add(-time.Duration(index+1) * time.Minute)
		item := &Notification{UserID: userID, Source: SourceSystem, Title: "Expired", Body: "Body", ExpiresAt: &expiresAt, CreatedAt: expiresAt.Add(-time.Hour)}
		if err := repository.Create(context.Background(), item); err != nil {
			t.Fatalf("Create() error = %v", err)
		}
	}
	deleted, err := repository.DeleteExpired(context.Background(), now, 1)
	if err != nil {
		t.Fatalf("DeleteExpired() error = %v", err)
	}
	if deleted != 1 {
		t.Fatalf("DeleteExpired() = %d, want 1", deleted)
	}
}

func newTestRepository(t *testing.T) (*Repository, int64, int64) {
	t.Helper()
	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	userRepository := users.NewRepository(db)
	now := time.Now().UTC()
	createUser := func(account string) int64 {
		user := &users.User{Account: account, Email: account + "@example.com", PasswordHash: "hash", DisplayName: account, Role: users.RolePlayer, Status: users.StatusActive, CreatedAt: now, UpdatedAt: now}
		if err := userRepository.Create(context.Background(), user); err != nil {
			t.Fatalf("create user %s: %v", account, err)
		}
		return user.ID
	}
	return NewRepository(db), createUser("first"), createUser("second")
}

func timePointer(value time.Time) *time.Time { return &value }
