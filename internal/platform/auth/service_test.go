package auth

import (
	"context"
	"errors"
	"testing"

	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestServiceRegistersNormalizedUserAndLogsIn(t *testing.T) {
	t.Parallel()

	repository := &memoryUsers{}
	service := NewService(repository, stubPasswords{})
	created, err := service.Register(context.Background(), RegisterRequest{
		Account: " Player.One ", Email: " PLAYER@Example.com ", Password: "long-password", DisplayName: " Player One ",
	})
	if err != nil {
		t.Fatalf("Register() error = %v", err)
	}
	if created.Account != "Player.One" || created.Email != "player@example.com" || created.DisplayName != "Player One" {
		t.Fatalf("created = %#v", created)
	}
	if created.PasswordHash != "hashed:long-password" {
		t.Fatalf("PasswordHash = %q", created.PasswordHash)
	}

	loggedIn, err := service.Authenticate(context.Background(), " player.one ", "long-password")
	if err != nil {
		t.Fatalf("Authenticate() error = %v", err)
	}
	if loggedIn.ID != created.ID {
		t.Fatalf("Authenticate().ID = %d, want %d", loggedIn.ID, created.ID)
	}
}

func TestServiceRejectsInvalidRegistrationAndCredentials(t *testing.T) {
	t.Parallel()

	service := NewService(&memoryUsers{}, stubPasswords{})
	for _, request := range []RegisterRequest{
		{Account: "x", Email: "player@example.com", Password: "long-password", DisplayName: "Player"},
		{Account: "player", Email: "not-email", Password: "long-password", DisplayName: "Player"},
		{Account: "player", Email: "player@example.com", Password: "short", DisplayName: "Player"},
		{Account: "bad account", Email: "player@example.com", Password: "long-password", DisplayName: "Player"},
	} {
		if _, err := service.Register(context.Background(), request); !errors.Is(err, ErrInvalidInput) {
			t.Fatalf("Register(%#v) error = %v, want ErrInvalidInput", request, err)
		}
	}
	if _, err := service.Authenticate(context.Background(), "missing", "long-password"); !errors.Is(err, ErrInvalidCredentials) {
		t.Fatalf("Authenticate(missing) error = %v", err)
	}
}

func TestServiceRejectsSuspendedUser(t *testing.T) {
	t.Parallel()

	repository := &memoryUsers{items: []*users.User{{ID: 1, Account: "player", PasswordHash: "hashed:long-password", Status: users.StatusSuspended}}}
	service := NewService(repository, stubPasswords{})
	if _, err := service.Authenticate(context.Background(), "player", "long-password"); !errors.Is(err, ErrAccountSuspended) {
		t.Fatalf("Authenticate() error = %v, want ErrAccountSuspended", err)
	}
}

func TestServiceBootstrapsAdministratorIdempotently(t *testing.T) {
	t.Parallel()
	repository := &memoryUsers{}
	service := NewService(repository, stubPasswords{})
	request := RegisterRequest{Account: "admin", Email: "admin@example.com", Password: "first-password", DisplayName: "Admin"}
	first, err := service.BootstrapAdministrator(context.Background(), request)
	if err != nil {
		t.Fatalf("BootstrapAdministrator() error = %v", err)
	}
	request.Password = "rotated-password"
	second, err := service.BootstrapAdministrator(context.Background(), request)
	if err != nil {
		t.Fatalf("BootstrapAdministrator() repeat error = %v", err)
	}
	if first.ID != second.ID || second.Role != users.RoleAdmin || second.PasswordHash != "hashed:rotated-password" {
		t.Fatalf("bootstrapped administrator = %#v", second)
	}
}

type memoryUsers struct {
	items []*users.User
}

func (m *memoryUsers) Create(_ context.Context, user *users.User) error {
	for _, item := range m.items {
		if item.Account == user.Account || item.Email == user.Email {
			return users.ErrConflict
		}
	}
	copy := *user
	copy.ID = int64(len(m.items) + 1)
	user.ID = copy.ID
	m.items = append(m.items, &copy)
	return nil
}

func (m *memoryUsers) UpsertAdministrator(_ context.Context, user *users.User) error {
	for index, item := range m.items {
		if equalFold(item.Account, user.Account) {
			copy := *user
			copy.ID = item.ID
			copy.Role = users.RoleAdmin
			m.items[index] = &copy
			*user = copy
			return nil
		}
		if equalFold(item.Email, user.Email) {
			return users.ErrConflict
		}
	}
	user.Role = users.RoleAdmin
	return m.Create(context.Background(), user)
}

func (m *memoryUsers) FindByID(_ context.Context, id int64) (*users.User, error) {
	for _, item := range m.items {
		if item.ID == id {
			copy := *item
			return &copy, nil
		}
	}
	return nil, users.ErrNotFound
}

func (m *memoryUsers) FindByAccount(_ context.Context, account string) (*users.User, error) {
	for _, item := range m.items {
		if equalFold(item.Account, account) {
			copy := *item
			return &copy, nil
		}
	}
	return nil, users.ErrNotFound
}

func (m *memoryUsers) FindByEmail(_ context.Context, email string) (*users.User, error) {
	for _, item := range m.items {
		if equalFold(item.Email, email) {
			copy := *item
			return &copy, nil
		}
	}
	return nil, users.ErrNotFound
}

type stubPasswords struct{}

func (stubPasswords) Hash(password string) (string, error) { return "hashed:" + password, nil }
func (stubPasswords) Verify(password, encoded string) (bool, error) {
	return encoded == "hashed:"+password, nil
}

func equalFold(left, right string) bool {
	if len(left) != len(right) {
		return false
	}
	for index := range left {
		leftByte, rightByte := left[index], right[index]
		if leftByte >= 'A' && leftByte <= 'Z' {
			leftByte += 'a' - 'A'
		}
		if rightByte >= 'A' && rightByte <= 'Z' {
			rightByte += 'a' - 'A'
		}
		if leftByte != rightByte {
			return false
		}
	}
	return true
}
