package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/downfa11/resistance-backend/internal/resistance/domain"
)

type DBTX interface {
	ExecContext(context.Context, string, ...any) (sql.Result, error)
	QueryContext(context.Context, string, ...any) (*sql.Rows, error)
	QueryRowContext(context.Context, string, ...any) *sql.Row
}

type Repository struct{ db DBTX }

func New(db DBTX) *Repository { return &Repository{db: db} }

func (r *Repository) EnsureProfile(ctx context.Context, userID int64, address string, now time.Time) (*domain.Profile, error) {
	stamp := formatTime(now)
	if _, err := r.db.ExecContext(ctx, `INSERT INTO resistance_profiles (user_id, address, created_at, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(user_id) DO NOTHING`, userID, address, stamp, stamp); err != nil {
		return nil, fmt.Errorf("ensure resistance profile: %w", err)
	}
	return r.FindProfile(ctx, userID)
}

const profileColumns = `p.user_id, u.display_name, p.address, p.gold, p.high_score, p.energy, p.scenario, p.head, p.body, p.arm, p.health, p.attack, p.critical, p.durability, p.created_at, p.updated_at`

func (r *Repository) FindProfile(ctx context.Context, userID int64) (*domain.Profile, error) {
	var item domain.Profile
	var created, updated string
	err := r.db.QueryRowContext(ctx, `SELECT `+profileColumns+` FROM resistance_profiles p JOIN users u ON u.id = p.user_id WHERE p.user_id = ?`, userID).
		Scan(&item.UserID, &item.DisplayName, &item.Address, &item.Gold, &item.HighScore, &item.Energy, &item.Scenario, &item.Head, &item.Body, &item.Arm, &item.Health, &item.Attack, &item.Critical, &item.Durability, &created, &updated)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, domain.ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("find resistance profile: %w", err)
	}
	item.CreatedAt, err = parseTime(created)
	if err != nil {
		return nil, err
	}
	item.UpdatedAt, err = parseTime(updated)
	if err != nil {
		return nil, err
	}
	return &item, nil
}

func (r *Repository) UpdateProfile(ctx context.Context, item domain.Profile, now time.Time) (*domain.Profile, error) {
	result, err := r.db.ExecContext(ctx, `UPDATE resistance_profiles SET address = ?, high_score = ?, energy = ?, scenario = ?, head = ?, body = ?, arm = ?, health = ?, attack = ?, critical = ?, durability = ?, updated_at = ? WHERE user_id = ?`, item.Address, item.HighScore, item.Energy, item.Scenario, item.Head, item.Body, item.Arm, item.Health, item.Attack, item.Critical, item.Durability, formatTime(now), item.UserID)
	if err != nil {
		return nil, fmt.Errorf("update resistance profile: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return nil, domain.ErrNotFound
	}
	return r.FindProfile(ctx, item.UserID)
}

func (r *Repository) PatchProfile(ctx context.Context, userID int64, patch domain.ProfilePatch, now time.Time) (*domain.Profile, error) {
	sets := make([]string, 0, 12)
	args := make([]any, 0, 13)
	add := func(column string, value any) {
		sets = append(sets, column+" = ?")
		args = append(args, value)
	}
	if patch.Address != nil {
		add("address", *patch.Address)
	}
	if patch.HighScore != nil {
		add("high_score", *patch.HighScore)
	}
	if patch.Energy != nil {
		add("energy", *patch.Energy)
	}
	if patch.Scenario != nil {
		add("scenario", *patch.Scenario)
	}
	if patch.Head != nil {
		add("head", *patch.Head)
	}
	if patch.Body != nil {
		add("body", *patch.Body)
	}
	if patch.Arm != nil {
		add("arm", *patch.Arm)
	}
	if patch.Health != nil {
		add("health", *patch.Health)
	}
	if patch.Attack != nil {
		add("attack", *patch.Attack)
	}
	if patch.Critical != nil {
		add("critical", *patch.Critical)
	}
	if patch.Durability != nil {
		add("durability", *patch.Durability)
	}
	if len(sets) == 0 {
		return r.FindProfile(ctx, userID)
	}
	add("updated_at", formatTime(now))
	args = append(args, userID)
	result, err := r.db.ExecContext(ctx, `UPDATE resistance_profiles SET `+strings.Join(sets, ", ")+` WHERE user_id = ?`, args...)
	if err != nil {
		return nil, fmt.Errorf("patch resistance profile: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return nil, domain.ErrNotFound
	}
	return r.FindProfile(ctx, userID)
}

type Ally struct {
	UserID                               int64
	DisplayName                          string
	HighScore                            int
	Head, Body, Arm                      int
	Health, Attack, Critical, Durability int
}

func (r *Repository) RequestFriend(ctx context.Context, requesterID, addresseeID int64, now time.Time) error {
	low, high, err := domain.CanonicalFriendPair(requesterID, addresseeID)
	if err != nil {
		return err
	}
	var exists int
	err = r.db.QueryRowContext(ctx, `SELECT EXISTS(SELECT 1 FROM resistance_profiles WHERE user_id = ?)`, addresseeID).Scan(&exists)
	if err != nil {
		return fmt.Errorf("check resistance addressee: %w", err)
	}
	if exists == 0 {
		return domain.ErrNotFound
	}
	err = r.db.QueryRowContext(ctx, `SELECT EXISTS(SELECT 1 FROM resistance_friendships WHERE user_low_id = ? AND user_high_id = ?)`, low, high).Scan(&exists)
	if err != nil {
		return fmt.Errorf("check resistance friendship: %w", err)
	}
	if exists != 0 {
		return domain.ErrConflict
	}
	result, err := r.db.ExecContext(ctx, `INSERT INTO resistance_friend_requests (requester_id, addressee_id, created_at) VALUES (?, ?, ?) ON CONFLICT(requester_id, addressee_id) DO NOTHING`, requesterID, addresseeID, formatTime(now))
	if err != nil {
		return fmt.Errorf("request resistance friend: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrConflict
	}
	return nil
}

func (r *Repository) AcceptFriend(ctx context.Context, addresseeID, requesterID int64, now time.Time) error {
	low, high, err := domain.CanonicalFriendPair(addresseeID, requesterID)
	if err != nil {
		return err
	}
	result, err := r.db.ExecContext(ctx, `DELETE FROM resistance_friend_requests WHERE requester_id = ? AND addressee_id = ?`, requesterID, addresseeID)
	if err != nil {
		return fmt.Errorf("accept resistance friend request: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrNotFound
	}
	if _, err := r.db.ExecContext(ctx, `DELETE FROM resistance_friend_requests WHERE requester_id = ? AND addressee_id = ?`, addresseeID, requesterID); err != nil {
		return fmt.Errorf("delete reverse friend request: %w", err)
	}
	if _, err := r.db.ExecContext(ctx, `INSERT INTO resistance_friendships (user_low_id, user_high_id, created_at) VALUES (?, ?, ?)`, low, high, formatTime(now)); err != nil {
		return fmt.Errorf("create resistance friendship: %w", err)
	}
	return nil
}

func (r *Repository) DeleteFriend(ctx context.Context, userID, otherID int64) error {
	low, high, err := domain.CanonicalFriendPair(userID, otherID)
	if err != nil {
		return err
	}
	result, err := r.db.ExecContext(ctx, `DELETE FROM resistance_friendships WHERE user_low_id = ? AND user_high_id = ?`, low, high)
	if err != nil {
		return fmt.Errorf("delete resistance friendship: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return domain.ErrNotFound
	}
	return nil
}

func (r *Repository) Friends(ctx context.Context, userID int64) ([]Ally, error) {
	return r.allies(ctx, `
		SELECT u.id, u.display_name, p.high_score, p.head, p.body, p.arm, p.health, p.attack, p.critical, p.durability
		FROM resistance_friendships f
		JOIN users u ON u.id = CASE WHEN f.user_low_id = ? THEN f.user_high_id ELSE f.user_low_id END
		JOIN resistance_profiles p ON p.user_id = u.id
		WHERE f.user_low_id = ? OR f.user_high_id = ? ORDER BY u.id`, userID, userID, userID)
}

func (r *Repository) IncomingRequests(ctx context.Context, userID int64) ([]Ally, error) {
	return r.allies(ctx, `
		SELECT u.id, u.display_name, p.high_score, p.head, p.body, p.arm, p.health, p.attack, p.critical, p.durability
		FROM resistance_friend_requests req JOIN users u ON u.id = req.requester_id JOIN resistance_profiles p ON p.user_id = u.id
		WHERE req.addressee_id = ? ORDER BY req.created_at DESC`, userID)
}

func (r *Repository) RandomAllies(ctx context.Context, userID int64, limit int) ([]Ally, error) {
	return r.allies(ctx, `
		SELECT u.id, u.display_name, p.high_score, p.head, p.body, p.arm, p.health, p.attack, p.critical, p.durability
		FROM resistance_profiles p JOIN users u ON u.id = p.user_id
		WHERE p.user_id <> ? AND NOT EXISTS (SELECT 1 FROM resistance_friendships f WHERE f.user_low_id = min(p.user_id, ?) AND f.user_high_id = max(p.user_id, ?))
		ORDER BY RANDOM() LIMIT ?`, userID, userID, userID, limit)
}

func (r *Repository) allies(ctx context.Context, query string, args ...any) ([]Ally, error) {
	rows, err := r.db.QueryContext(ctx, query, args...)
	if err != nil {
		return nil, fmt.Errorf("list resistance allies: %w", err)
	}
	defer rows.Close()
	items := make([]Ally, 0)
	for rows.Next() {
		var item Ally
		if err := rows.Scan(&item.UserID, &item.DisplayName, &item.HighScore, &item.Head, &item.Body, &item.Arm, &item.Health, &item.Attack, &item.Critical, &item.Durability); err != nil {
			return nil, fmt.Errorf("scan resistance ally: %w", err)
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

func formatTime(value time.Time) string { return value.UTC().Format(time.RFC3339Nano) }
func parseTime(value string) (time.Time, error) {
	parsed, err := time.Parse(time.RFC3339Nano, value)
	if err != nil {
		return time.Time{}, fmt.Errorf("parse resistance time: %w", err)
	}
	return parsed, nil
}
