package auth

import (
	"context"
	"errors"
	"fmt"
	"net/mail"
	"regexp"
	"strings"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/users"
)

var (
	ErrInvalidInput       = errors.New("invalid authentication input")
	ErrAlreadyExists      = errors.New("account or email already exists")
	ErrInvalidCredentials = errors.New("invalid credentials")
	ErrAccountSuspended   = errors.New("account is suspended")
)

var accountPattern = regexp.MustCompile(`^[A-Za-z0-9_.-]{3,32}$`)

type UserRepository interface {
	Create(context.Context, *users.User) error
	FindByID(context.Context, int64) (*users.User, error)
	FindByAccount(context.Context, string) (*users.User, error)
	FindByEmail(context.Context, string) (*users.User, error)
}

type Passwords interface {
	Hash(string) (string, error)
	Verify(string, string) (bool, error)
}

type Service struct {
	users     UserRepository
	passwords Passwords
	now       func() time.Time
}

type RegisterRequest struct {
	Account     string
	Email       string
	Password    string
	DisplayName string
}

func NewService(repository UserRepository, passwords Passwords) *Service {
	return &Service{users: repository, passwords: passwords, now: time.Now}
}

func (s *Service) Register(ctx context.Context, request RegisterRequest) (*users.User, error) {
	request.Account = strings.TrimSpace(request.Account)
	request.Email = strings.ToLower(strings.TrimSpace(request.Email))
	request.DisplayName = strings.TrimSpace(request.DisplayName)
	if !validRegistration(request) {
		return nil, ErrInvalidInput
	}
	hash, err := s.passwords.Hash(request.Password)
	if err != nil {
		return nil, fmt.Errorf("hash password: %w", err)
	}
	now := s.now().UTC()
	user := &users.User{
		Account:      request.Account,
		Email:        request.Email,
		PasswordHash: hash,
		DisplayName:  request.DisplayName,
		Role:         users.RolePlayer,
		Status:       users.StatusActive,
		CreatedAt:    now,
		UpdatedAt:    now,
	}
	if err := s.users.Create(ctx, user); err != nil {
		if errors.Is(err, users.ErrConflict) {
			return nil, ErrAlreadyExists
		}
		return nil, fmt.Errorf("create user: %w", err)
	}
	return user, nil
}

func (s *Service) Authenticate(ctx context.Context, account, password string) (*users.User, error) {
	account = strings.TrimSpace(account)
	if account == "" || password == "" {
		return nil, ErrInvalidCredentials
	}
	user, err := s.users.FindByAccount(ctx, account)
	if err != nil {
		if errors.Is(err, users.ErrNotFound) {
			return nil, ErrInvalidCredentials
		}
		return nil, fmt.Errorf("find user: %w", err)
	}
	matched, err := s.passwords.Verify(password, user.PasswordHash)
	if err != nil {
		return nil, fmt.Errorf("verify password: %w", err)
	}
	if !matched {
		return nil, ErrInvalidCredentials
	}
	if user.Status != users.StatusActive {
		return nil, ErrAccountSuspended
	}
	return user, nil
}

func validRegistration(request RegisterRequest) bool {
	if !accountPattern.MatchString(request.Account) || len(request.Password) < 8 || len(request.Password) > 128 {
		return false
	}
	if request.DisplayName == "" || len([]rune(request.DisplayName)) > 80 {
		return false
	}
	parsed, err := mail.ParseAddress(request.Email)
	return err == nil && strings.EqualFold(parsed.Address, request.Email)
}
