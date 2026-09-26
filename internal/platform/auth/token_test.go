package auth

import (
	"strings"
	"testing"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestTokenManagerIssuesAndParsesAccessToken(t *testing.T) {
	t.Parallel()

	manager, err := NewTokenManager("test-signing-secret", 15*time.Minute)
	if err != nil {
		t.Fatalf("NewTokenManager() error = %v", err)
	}
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	token, err := manager.Issue(42, users.RoleAdmin, "session-1", now)
	if err != nil {
		t.Fatalf("Issue() error = %v", err)
	}
	subject, err := manager.Parse(token, now.Add(time.Minute))
	if err != nil {
		t.Fatalf("Parse() error = %v", err)
	}
	if subject.UserID != 42 || subject.Role != users.RoleAdmin || subject.SessionID != "session-1" {
		t.Fatalf("subject = %#v", subject)
	}
}

func TestTokenManagerRejectsTamperedAndExpiredTokens(t *testing.T) {
	t.Parallel()

	manager, err := NewTokenManager("test-signing-secret", time.Minute)
	if err != nil {
		t.Fatalf("NewTokenManager() error = %v", err)
	}
	now := time.Date(2026, 9, 21, 0, 0, 0, 0, time.UTC)
	token, err := manager.Issue(1, users.RolePlayer, "session-1", now)
	if err != nil {
		t.Fatalf("Issue() error = %v", err)
	}
	if _, err := manager.Parse(token+"tampered", now); err == nil {
		t.Fatal("Parse(tampered) error = nil")
	}
	if _, err := manager.Parse(token, now.Add(2*time.Minute)); err == nil {
		t.Fatal("Parse(expired) error = nil")
	}
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		t.Fatalf("JWT parts = %d", len(parts))
	}
}
