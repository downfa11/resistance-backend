package auth

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/base64"
	"fmt"
	"io"
	"strings"

	"golang.org/x/crypto/argon2"
)

type PasswordParams struct {
	Memory      uint32
	Iterations  uint32
	Parallelism uint8
	SaltLength  uint32
	KeyLength   uint32
}

func DefaultPasswordParams() PasswordParams {
	return PasswordParams{
		Memory:      64 * 1024,
		Iterations:  3,
		Parallelism: 2,
		SaltLength:  16,
		KeyLength:   32,
	}
}

type PasswordHasher struct {
	params PasswordParams
	random io.Reader
}

func NewPasswordHasher(params PasswordParams) *PasswordHasher {
	return &PasswordHasher{params: params, random: rand.Reader}
}

func (h *PasswordHasher) Hash(password string) (string, error) {
	if h == nil || h.random == nil || !validPasswordParams(h.params) {
		return "", fmt.Errorf("invalid password hasher configuration")
	}
	salt := make([]byte, h.params.SaltLength)
	if _, err := io.ReadFull(h.random, salt); err != nil {
		return "", fmt.Errorf("generate password salt: %w", err)
	}
	hash := argon2.IDKey([]byte(password), salt, h.params.Iterations, h.params.Memory, h.params.Parallelism, h.params.KeyLength)
	return fmt.Sprintf(
		"$argon2id$v=%d$m=%d,t=%d,p=%d$%s$%s",
		argon2.Version,
		h.params.Memory,
		h.params.Iterations,
		h.params.Parallelism,
		base64.RawStdEncoding.EncodeToString(salt),
		base64.RawStdEncoding.EncodeToString(hash),
	), nil
}

func (h *PasswordHasher) Verify(password, encoded string) (bool, error) {
	params, salt, expected, err := parsePasswordHash(encoded)
	if err != nil {
		return false, err
	}
	actual := argon2.IDKey([]byte(password), salt, params.Iterations, params.Memory, params.Parallelism, params.KeyLength)
	return subtle.ConstantTimeCompare(actual, expected) == 1, nil
}

func parsePasswordHash(encoded string) (PasswordParams, []byte, []byte, error) {
	parts := strings.Split(encoded, "$")
	if len(parts) != 6 || parts[0] != "" || parts[1] != "argon2id" || parts[2] != fmt.Sprintf("v=%d", argon2.Version) {
		return PasswordParams{}, nil, nil, fmt.Errorf("invalid argon2id encoding")
	}
	var params PasswordParams
	if _, err := fmt.Sscanf(parts[3], "m=%d,t=%d,p=%d", &params.Memory, &params.Iterations, &params.Parallelism); err != nil {
		return PasswordParams{}, nil, nil, fmt.Errorf("parse argon2id parameters: %w", err)
	}
	salt, err := base64.RawStdEncoding.DecodeString(parts[4])
	if err != nil {
		return PasswordParams{}, nil, nil, fmt.Errorf("decode argon2id salt: %w", err)
	}
	expected, err := base64.RawStdEncoding.DecodeString(parts[5])
	if err != nil {
		return PasswordParams{}, nil, nil, fmt.Errorf("decode argon2id hash: %w", err)
	}
	params.SaltLength = uint32(len(salt))
	params.KeyLength = uint32(len(expected))
	if !validPasswordParams(params) {
		return PasswordParams{}, nil, nil, fmt.Errorf("invalid argon2id parameters")
	}
	return params, salt, expected, nil
}

func validPasswordParams(params PasswordParams) bool {
	return params.Memory >= 8*1024 && params.Memory <= 1024*1024 &&
		params.Iterations >= 1 && params.Iterations <= 10 &&
		params.Parallelism >= 1 && params.Parallelism <= 16 &&
		params.SaltLength >= 16 && params.SaltLength <= 64 &&
		params.KeyLength >= 16 && params.KeyLength <= 64
}
