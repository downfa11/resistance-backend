package service

import (
	"context"
	"database/sql"
	"errors"
	"path/filepath"
	"testing"
	"time"

	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
	"github.com/downfa11/resistance-backend/internal/resistance/domain"
)

func TestProfileCreationIsIdempotentAndPatchIsExplicit(t *testing.T) {
	db := openTestDB(t)
	userID := insertUser(t, db, "profile")
	service := New(db)
	first, err := service.EnsureProfile(context.Background(), userID, "first")
	if err != nil {
		t.Fatalf("ensure profile: %v", err)
	}
	second, err := service.EnsureProfile(context.Background(), userID, "ignored")
	if err != nil {
		t.Fatalf("ensure profile again: %v", err)
	}
	if second.Address != first.Address {
		t.Fatalf("idempotent address = %q", second.Address)
	}
	energy := 80
	patched, err := service.PatchProfile(context.Background(), userID, domain.ProfilePatch{Energy: &energy})
	if err != nil {
		t.Fatalf("patch profile: %v", err)
	}
	if patched.Energy != 80 || patched.Address != "first" || patched.Gold != 0 {
		t.Fatalf("patched = %#v", patched)
	}
}

func TestFriendRequestAcceptanceIsCanonicalAndNotifies(t *testing.T) {
	db := openTestDB(t)
	firstID := insertUser(t, db, "first")
	secondID := insertUser(t, db, "second")
	service := New(db)
	if _, err := service.EnsureProfile(context.Background(), firstID, ""); err != nil {
		t.Fatal(err)
	}
	if _, err := service.EnsureProfile(context.Background(), secondID, ""); err != nil {
		t.Fatal(err)
	}
	if err := service.RequestFriend(context.Background(), firstID, secondID); err != nil {
		t.Fatalf("request friend: %v", err)
	}
	if err := service.RequestFriend(context.Background(), firstID, secondID); !errors.Is(err, domain.ErrConflict) {
		t.Fatalf("duplicate request error = %v", err)
	}
	requests, err := service.FriendRequests(context.Background(), secondID)
	if err != nil || len(requests) != 1 || requests[0].UserID != firstID {
		t.Fatalf("requests = %#v, %v", requests, err)
	}
	if err := service.AcceptFriend(context.Background(), secondID, firstID); err != nil {
		t.Fatalf("accept friend: %v", err)
	}
	for _, userID := range []int64{firstID, secondID} {
		friends, err := service.Friends(context.Background(), userID)
		if err != nil || len(friends) != 1 {
			t.Fatalf("friends for %d = %#v, %v", userID, friends, err)
		}
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE source = 'RESISTANCE'`).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 2 {
		t.Fatalf("notification count = %d", count)
	}
	if err := service.DeleteFriend(context.Background(), firstID, secondID); err != nil {
		t.Fatalf("delete friend: %v", err)
	}
}

func TestExchangeIsExactlyOnceAndKeepsResistanceWalletIsolated(t *testing.T) {
	db := openTestDB(t)
	userID := insertUser(t, db, "exchange")
	service := New(db)
	if _, err := service.EnsureProfile(context.Background(), userID, ""); err != nil {
		t.Fatal(err)
	}
	if err := service.SetBalance(context.Background(), userID, "JPY", 10); err != nil {
		t.Fatal(err)
	}
	first, err := service.Exchange(context.Background(), userID, "JPY", 2, "exchange-1")
	if err != nil {
		t.Fatalf("exchange: %v", err)
	}
	second, err := service.Exchange(context.Background(), userID, "JPY", 2, "exchange-1")
	if err != nil {
		t.Fatalf("repeat exchange: %v", err)
	}
	if first.ID != second.ID {
		t.Fatalf("idempotent exchange ids = %d, %d", first.ID, second.ID)
	}
	if _, err := service.Exchange(context.Background(), userID, "JPY", 3, "exchange-1"); !errors.Is(err, domain.ErrIdempotencyConflict) {
		t.Fatalf("conflicting exchange error = %v", err)
	}
	profile, err := service.Profile(context.Background(), userID)
	if err != nil {
		t.Fatal(err)
	}
	if profile.Gold != first.GoldGranted {
		t.Fatalf("gold = %d", profile.Gold)
	}
	balances, err := service.Balances(context.Background(), userID)
	if err != nil {
		t.Fatal(err)
	}
	for _, balance := range balances {
		if balance.Code == "JPY" && balance.Quantity != 8 {
			t.Fatalf("JPY balance = %d", balance.Quantity)
		}
	}
	var notifications int
	if err := db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE user_id = ? AND title = 'Exchange complete'`, userID).Scan(&notifications); err != nil {
		t.Fatal(err)
	}
	if notifications != 1 {
		t.Fatalf("exchange notifications = %d", notifications)
	}
}

func TestAdminRateAndSupporterCodeManagement(t *testing.T) {
	db := openTestDB(t)
	service := New(db)
	if _, err := db.Exec(`UPDATE resistance_currency_rates SET rate = 100, uses = CASE code WHEN 'JPY' THEN 25 ELSE 15 END`); err != nil {
		t.Fatal(err)
	}
	adjusted, err := service.AdjustRates(context.Background())
	if err != nil {
		t.Fatalf("adjust rates: %v", err)
	}
	for _, item := range adjusted {
		if item.Uses != 0 || item.Rate < 1 {
			t.Fatalf("adjusted rate = %#v", item)
		}
	}
	reset, err := service.ResetRates(context.Background())
	if err != nil {
		t.Fatalf("reset rates: %v", err)
	}
	for _, item := range reset {
		if item.Rate != domain.DefaultCurrencyRates[item.Code] || item.Uses != 0 {
			t.Fatalf("reset rate = %#v", item)
		}
	}
	created, err := service.CreateSupporterCode(context.Background(), "founder", "DELETE-ME", 10)
	if err != nil {
		t.Fatal(err)
	}
	codes, err := service.SupporterCodes(context.Background(), 10)
	if err != nil || len(codes) != 1 || codes[0].ID != created.ID {
		t.Fatalf("supporter codes = %#v, %v", codes, err)
	}
	if err := service.DeleteSupporterCode(context.Background(), created.ID); err != nil {
		t.Fatalf("delete supporter code: %v", err)
	}
	if err := service.DeleteSupporterCode(context.Background(), created.ID); !errors.Is(err, domain.ErrConflict) {
		t.Fatalf("delete missing supporter code error = %v", err)
	}
}

func TestSupporterRedemptionAndContentPublication(t *testing.T) {
	db := openTestDB(t)
	adminID := insertUser(t, db, "admin")
	userID := insertUser(t, db, "supporter")
	service := New(db)
	if _, err := service.EnsureProfile(context.Background(), userID, ""); err != nil {
		t.Fatal(err)
	}
	if _, err := service.CreateSupporterCode(context.Background(), "founder", "WELCOME", 250); err != nil {
		t.Fatal(err)
	}
	first, err := service.RedeemSupporterCode(context.Background(), userID, "WELCOME", "redeem-1")
	if err != nil {
		t.Fatalf("redeem: %v", err)
	}
	second, err := service.RedeemSupporterCode(context.Background(), userID, "WELCOME", "redeem-1")
	if err != nil || first.RewardGold != second.RewardGold {
		t.Fatalf("repeat redemption = %#v, %v", second, err)
	}
	if _, err := service.RedeemSupporterCode(context.Background(), userID, "WELCOME", "redeem-2"); !errors.Is(err, domain.ErrConflict) {
		t.Fatalf("second key error = %v", err)
	}
	manifest, err := service.PublishContent(context.Background(), adminID, "v1", []domain.ContentEntry{{ChapterID: "chapter-1", ID: "dialog-1", MediaType: "text/plain", Body: "hello"}})
	if err != nil {
		t.Fatalf("publish content: %v", err)
	}
	loaded, err := service.Content(context.Background())
	if err != nil {
		t.Fatalf("load content: %v", err)
	}
	if loaded.Version != manifest.Version || loaded.Entries[0].SHA256 == "" {
		t.Fatalf("manifest = %#v", loaded)
	}
	if _, err := service.PublishNotice(context.Background(), adminID, "Server news", "Hello Resistance"); err != nil {
		t.Fatalf("publish notice: %v", err)
	}
	var count int
	if err := db.QueryRow(`SELECT COUNT(*) FROM notifications WHERE user_id = ? AND source = 'RESISTANCE'`, userID).Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 2 {
		t.Fatalf("supporter plus notice notifications = %d", count)
	}
}

func openTestDB(t *testing.T) *sql.DB {
	t.Helper()
	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatal(err)
	}
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { db.Close() })
	return db
}

func insertUser(t *testing.T, db *sql.DB, suffix string) int64 {
	t.Helper()
	now := time.Now().UTC().Format(time.RFC3339Nano)
	result, err := db.Exec(`INSERT INTO users (account, email, password_hash, display_name, role, status, created_at, updated_at) VALUES (?, ?, 'hash', ?, 'PLAYER', 'ACTIVE', ?, ?)`, suffix, suffix+"@example.com", suffix, now, now)
	if err != nil {
		t.Fatal(err)
	}
	id, err := result.LastInsertId()
	if err != nil {
		t.Fatal(err)
	}
	return id
}
