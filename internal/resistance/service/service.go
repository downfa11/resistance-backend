package service

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/notifications"
	"github.com/downfa11/resistance-backend/internal/resistance/domain"
	"github.com/downfa11/resistance-backend/internal/resistance/repository"
)

type Service struct {
	db   *sql.DB
	repo *repository.Repository
	now  func() time.Time
}

func New(db *sql.DB) *Service { return &Service{db: db, repo: repository.New(db), now: time.Now} }

func (s *Service) EnsureProfile(ctx context.Context, userID int64, address string) (*domain.Profile, error) {
	address = strings.TrimSpace(address)
	profile := domain.Profile{UserID: userID, Address: address, Energy: 100, Health: 100, Attack: 10}
	if userID < 1 || !profile.Valid() {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.EnsureProfile(ctx, userID, address, s.now().UTC())
}

func (s *Service) Profile(ctx context.Context, userID int64) (*domain.Profile, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.FindProfile(ctx, userID)
}

func (s *Service) PatchProfile(ctx context.Context, userID int64, patch domain.ProfilePatch) (*domain.Profile, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	stored, err := s.repo.FindProfile(ctx, userID)
	if err != nil {
		return nil, err
	}
	updated, err := patch.Apply(*stored)
	if err != nil {
		return nil, err
	}
	return s.repo.UpdateProfile(ctx, updated, s.now().UTC())
}

func (s *Service) RequestFriend(ctx context.Context, userID, targetID int64) error {
	if userID < 1 || targetID < 1 {
		return domain.ErrInvalidInput
	}
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin friend request: %w", err)
	}
	defer tx.Rollback()
	repo := repository.New(tx)
	if _, err := repo.EnsureProfile(ctx, userID, "", now); err != nil {
		return err
	}
	if err := repo.RequestFriend(ctx, userID, targetID, now); err != nil {
		return err
	}
	if _, err := notifications.NewService(notifications.NewRepository(tx)).Create(ctx, notifications.CreateRequest{UserID: targetID, Source: notifications.SourceResistance, Title: "Friend request", Body: "You received a Resistance friend request.", DeepLink: "resistance://friends/requests"}); err != nil {
		return fmt.Errorf("create friend request notification: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("commit friend request: %w", err)
	}
	return nil
}

func (s *Service) AcceptFriend(ctx context.Context, userID, requesterID int64) error {
	if userID < 1 || requesterID < 1 {
		return domain.ErrInvalidInput
	}
	now := s.now().UTC()
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin friend acceptance: %w", err)
	}
	defer tx.Rollback()
	if err := repository.New(tx).AcceptFriend(ctx, userID, requesterID, now); err != nil {
		return err
	}
	if _, err := notifications.NewService(notifications.NewRepository(tx)).Create(ctx, notifications.CreateRequest{UserID: requesterID, Source: notifications.SourceResistance, Title: "Friend request accepted", Body: "Your Resistance friend request was accepted.", DeepLink: "resistance://friends"}); err != nil {
		return fmt.Errorf("create friend acceptance notification: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("commit friend acceptance: %w", err)
	}
	return nil
}

func (s *Service) DeleteFriend(ctx context.Context, userID, otherID int64) error {
	if userID < 1 || otherID < 1 {
		return domain.ErrInvalidInput
	}
	return s.repo.DeleteFriend(ctx, userID, otherID)
}

func (s *Service) Friends(ctx context.Context, userID int64) ([]repository.Ally, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.Friends(ctx, userID)
}
func (s *Service) FriendRequests(ctx context.Context, userID int64) ([]repository.Ally, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.IncomingRequests(ctx, userID)
}
func (s *Service) RandomAllies(ctx context.Context, userID int64) ([]repository.Ally, error) {
	if userID < 1 {
		return nil, domain.ErrInvalidInput
	}
	return s.repo.RandomAllies(ctx, userID, 3)
}
