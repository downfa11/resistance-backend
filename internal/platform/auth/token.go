package auth

import (
	"errors"
	"fmt"
	"strconv"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/users"
	"github.com/golang-jwt/jwt/v5"
)

const tokenIssuer = "resistance-server"

var ErrInvalidAccessToken = errors.New("invalid access token")

type Subject struct {
	UserID    int64
	Role      users.Role
	SessionID string
}

type accessClaims struct {
	Role      users.Role `json:"role"`
	SessionID string     `json:"sid"`
	jwt.RegisteredClaims
}

type TokenManager struct {
	secret    []byte
	accessTTL time.Duration
}

func NewTokenManager(secret string, accessTTL time.Duration) (*TokenManager, error) {
	if secret == "" || accessTTL <= 0 {
		return nil, fmt.Errorf("create token manager: secret and positive access TTL are required")
	}
	return &TokenManager{secret: []byte(secret), accessTTL: accessTTL}, nil
}

func (m *TokenManager) Issue(userID int64, role users.Role, sessionID string, now time.Time) (string, error) {
	if userID < 1 || sessionID == "" || (role != users.RolePlayer && role != users.RoleAdmin) {
		return "", ErrInvalidAccessToken
	}
	claims := accessClaims{
		Role:      role,
		SessionID: sessionID,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    tokenIssuer,
			Subject:   strconv.FormatInt(userID, 10),
			IssuedAt:  jwt.NewNumericDate(now.UTC()),
			ExpiresAt: jwt.NewNumericDate(now.UTC().Add(m.accessTTL)),
		},
	}
	encoded, err := jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString(m.secret)
	if err != nil {
		return "", fmt.Errorf("sign access token: %w", err)
	}
	return encoded, nil
}

func (m *TokenManager) Parse(encoded string, now time.Time) (Subject, error) {
	claims := &accessClaims{}
	token, err := jwt.ParseWithClaims(
		encoded,
		claims,
		func(token *jwt.Token) (any, error) {
			if token.Method != jwt.SigningMethodHS256 {
				return nil, ErrInvalidAccessToken
			}
			return m.secret, nil
		},
		jwt.WithIssuer(tokenIssuer),
		jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}),
		jwt.WithTimeFunc(func() time.Time { return now.UTC() }),
	)
	if err != nil || !token.Valid {
		return Subject{}, ErrInvalidAccessToken
	}
	userID, err := strconv.ParseInt(claims.Subject, 10, 64)
	if err != nil || userID < 1 || claims.SessionID == "" || (claims.Role != users.RolePlayer && claims.Role != users.RoleAdmin) {
		return Subject{}, ErrInvalidAccessToken
	}
	return Subject{UserID: userID, Role: claims.Role, SessionID: claims.SessionID}, nil
}
