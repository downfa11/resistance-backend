package notifications

import (
	"errors"
	"time"
)

var (
	ErrNotFound     = errors.New("notification not found")
	ErrInvalidInput = errors.New("invalid notification input")
)

type Source string

const (
	SourceSystem     Source = "SYSTEM"
	SourceResistance Source = "RESISTANCE"
)

type Notification struct {
	ID        int64
	UserID    int64
	Source    Source
	Title     string
	Body      string
	DeepLink  string
	ReadAt    *time.Time
	ExpiresAt *time.Time
	CreatedAt time.Time
}

func (s Source) Valid() bool {
	return s == SourceSystem || s == SourceResistance
}
