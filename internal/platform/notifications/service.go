package notifications

import (
	"context"
	"fmt"
	"strings"
	"time"
)

const (
	defaultListLimit = 20
	maximumListLimit = 100
)

type Store interface {
	Create(context.Context, *Notification) error
	List(context.Context, int64, int, int64, time.Time) ([]*Notification, error)
	Find(context.Context, int64, int64, time.Time) (*Notification, error)
	MarkRead(context.Context, int64, int64, time.Time) (*Notification, error)
	MarkAllRead(context.Context, int64, time.Time) (int64, error)
}

type Writer interface {
	Create(context.Context, CreateRequest) (*Notification, error)
}

type Service struct {
	store Store
	now   func() time.Time
}

type CreateRequest struct {
	UserID    int64
	Source    Source
	Title     string
	Body      string
	DeepLink  string
	ExpiresAt *time.Time
}

func NewService(store Store) *Service {
	return &Service{store: store, now: time.Now}
}

func (s *Service) Create(ctx context.Context, request CreateRequest) (*Notification, error) {
	request.Title = strings.TrimSpace(request.Title)
	request.Body = strings.TrimSpace(request.Body)
	request.DeepLink = strings.TrimSpace(request.DeepLink)
	if request.UserID < 1 || !request.Source.Valid() || request.Title == "" || len([]rune(request.Title)) > 160 || request.Body == "" || len([]rune(request.Body)) > 4000 {
		return nil, ErrInvalidInput
	}
	now := s.now().UTC()
	if request.ExpiresAt != nil && !request.ExpiresAt.After(now) {
		return nil, ErrInvalidInput
	}
	item := &Notification{
		UserID: request.UserID, Source: request.Source, Title: request.Title, Body: request.Body,
		DeepLink: request.DeepLink, ExpiresAt: request.ExpiresAt, CreatedAt: now,
	}
	if err := s.store.Create(ctx, item); err != nil {
		return nil, fmt.Errorf("create notification: %w", err)
	}
	return item, nil
}

func (s *Service) List(ctx context.Context, userID int64, limit int, beforeID int64) ([]*Notification, error) {
	if limit == 0 {
		limit = defaultListLimit
	}
	if userID < 1 || limit < 1 || limit > maximumListLimit || beforeID < 0 {
		return nil, ErrInvalidInput
	}
	return s.store.List(ctx, userID, limit, beforeID, s.now().UTC())
}

func (s *Service) Find(ctx context.Context, userID, id int64) (*Notification, error) {
	if userID < 1 || id < 1 {
		return nil, ErrInvalidInput
	}
	return s.store.Find(ctx, userID, id, s.now().UTC())
}

func (s *Service) MarkRead(ctx context.Context, userID, id int64) (*Notification, error) {
	if userID < 1 || id < 1 {
		return nil, ErrInvalidInput
	}
	return s.store.MarkRead(ctx, userID, id, s.now().UTC())
}

func (s *Service) MarkAllRead(ctx context.Context, userID int64) (int64, error) {
	if userID < 1 {
		return 0, ErrInvalidInput
	}
	return s.store.MarkAllRead(ctx, userID, s.now().UTC())
}
