package auth

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/users"
)

var ErrInvalidRefreshToken = errors.New("invalid refresh token")

type RefreshSessionStore interface {
	Create(context.Context, *RefreshSession) error
	FindByTokenHash(context.Context, []byte) (*RefreshSession, error)
	Rotate(context.Context, []byte, *RefreshSession, time.Time) (*RefreshSession, error)
	RevokeByTokenHash(context.Context, []byte, time.Time) error
}

type SessionBundle struct {
	AccessToken      string
	RefreshToken     string
	AccessExpiresAt  time.Time
	RefreshExpiresAt time.Time
	User             *users.User
}

type SessionService struct {
	sessions   RefreshSessionStore
	users      UserRepository
	tokens     *TokenManager
	refreshTTL time.Duration
	now        func() time.Time
	random     io.Reader
}

func NewSessionService(sessions RefreshSessionStore, userRepository UserRepository, tokens *TokenManager, refreshTTL time.Duration) *SessionService {
	return &SessionService{
		sessions: sessions, users: userRepository, tokens: tokens, refreshTTL: refreshTTL, now: time.Now, random: rand.Reader,
	}
}

func (s *SessionService) Start(ctx context.Context, user *users.User) (*SessionBundle, error) {
	if user == nil || user.ID < 1 || user.Status != users.StatusActive {
		return nil, ErrInvalidCredentials
	}
	now := s.now().UTC()
	rawToken, tokenHash, err := s.newRefreshToken()
	if err != nil {
		return nil, err
	}
	sessionID, err := s.newSessionID()
	if err != nil {
		return nil, err
	}
	session := &RefreshSession{
		ID: sessionID, FamilyID: sessionID, UserID: user.ID, TokenHash: tokenHash,
		ExpiresAt: now.Add(s.refreshTTL), CreatedAt: now,
	}
	if err := s.sessions.Create(ctx, session); err != nil {
		return nil, fmt.Errorf("create session: %w", err)
	}
	return s.bundle(user, session, rawToken, now)
}

func (s *SessionService) Refresh(ctx context.Context, rawToken string) (*SessionBundle, error) {
	if rawToken == "" {
		return nil, ErrInvalidRefreshToken
	}
	presentedHash := hashRefreshToken(rawToken)
	current, err := s.sessions.FindByTokenHash(ctx, presentedHash)
	if err != nil && !errors.Is(err, ErrSessionNotFound) {
		return nil, fmt.Errorf("find refresh session: %w", err)
	}
	if errors.Is(err, ErrSessionNotFound) {
		return nil, ErrInvalidRefreshToken
	}
	user, err := s.users.FindByID(ctx, current.UserID)
	if errors.Is(err, users.ErrNotFound) {
		return nil, ErrInvalidRefreshToken
	}
	if err != nil {
		return nil, fmt.Errorf("find refresh user: %w", err)
	}
	if user.Status != users.StatusActive {
		return nil, ErrInvalidRefreshToken
	}

	now := s.now().UTC()
	replacementRaw, replacementHash, err := s.newRefreshToken()
	if err != nil {
		return nil, err
	}
	replacementID, err := s.newSessionID()
	if err != nil {
		return nil, err
	}
	replacement := &RefreshSession{ID: replacementID, TokenHash: replacementHash, ExpiresAt: now.Add(s.refreshTTL), CreatedAt: now}
	rotated, err := s.sessions.Rotate(ctx, presentedHash, replacement, now)
	if err != nil {
		if errors.Is(err, ErrSessionNotFound) || errors.Is(err, ErrSessionExpired) || errors.Is(err, ErrSessionRevoked) || errors.Is(err, ErrSessionReused) {
			return nil, ErrInvalidRefreshToken
		}
		return nil, fmt.Errorf("rotate refresh session: %w", err)
	}
	return s.bundle(user, rotated, replacementRaw, now)
}

func (s *SessionService) Logout(ctx context.Context, rawToken string) error {
	if rawToken == "" {
		return nil
	}
	err := s.sessions.RevokeByTokenHash(ctx, hashRefreshToken(rawToken), s.now().UTC())
	if errors.Is(err, ErrSessionNotFound) {
		return nil
	}
	if err != nil {
		return fmt.Errorf("revoke refresh session: %w", err)
	}
	return nil
}

func (s *SessionService) bundle(user *users.User, session *RefreshSession, refreshToken string, now time.Time) (*SessionBundle, error) {
	accessToken, err := s.tokens.Issue(user.ID, user.Role, session.ID, now)
	if err != nil {
		return nil, err
	}
	userCopy := *user
	return &SessionBundle{
		AccessToken: accessToken, RefreshToken: refreshToken,
		AccessExpiresAt: now.Add(s.tokens.accessTTL), RefreshExpiresAt: session.ExpiresAt, User: &userCopy,
	}, nil
}

func (s *SessionService) newRefreshToken() (string, []byte, error) {
	value := make([]byte, 32)
	if _, err := io.ReadFull(s.random, value); err != nil {
		return "", nil, fmt.Errorf("generate refresh token: %w", err)
	}
	raw := base64.RawURLEncoding.EncodeToString(value)
	return raw, hashRefreshToken(raw), nil
}

func (s *SessionService) newSessionID() (string, error) {
	value := make([]byte, 16)
	if _, err := io.ReadFull(s.random, value); err != nil {
		return "", fmt.Errorf("generate session ID: %w", err)
	}
	return hex.EncodeToString(value), nil
}

func hashRefreshToken(raw string) []byte {
	hash := sha256.Sum256([]byte(raw))
	return hash[:]
}
