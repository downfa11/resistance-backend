package users

import (
	"errors"
	"time"
)

var (
	ErrNotFound = errors.New("user not found")
	ErrConflict = errors.New("user account or email already exists")
)

type Role string

const (
	RolePlayer Role = "PLAYER"
	RoleAdmin  Role = "ADMIN"
)

type Status string

const (
	StatusActive    Status = "ACTIVE"
	StatusSuspended Status = "SUSPENDED"
)

type User struct {
	ID           int64
	Account      string
	Email        string
	PasswordHash string
	DisplayName  string
	Role         Role
	Status       Status
	CreatedAt    time.Time
	UpdatedAt    time.Time
}
