package domain

import (
	"errors"
	"math"
	"strings"
	"time"
)

var (
	ErrNotFound             = errors.New("resistance resource not found")
	ErrInvalidInput         = errors.New("invalid resistance input")
	ErrConflict             = errors.New("resistance resource conflict")
	ErrInsufficientCurrency = errors.New("insufficient resistance currency")
	ErrIdempotencyConflict  = errors.New("idempotency key reused with different input")
)

type Profile struct {
	UserID                               int64
	DisplayName                          string
	Address                              string
	Gold                                 int64
	HighScore, Energy                    int
	Scenario, Head, Body, Arm            int
	Health, Attack, Critical, Durability int
	CreatedAt, UpdatedAt                 time.Time
}

type ProfilePatch struct {
	Address                              *string
	HighScore                            *int
	Energy                               *int
	Scenario, Head, Body, Arm            *int
	Health, Attack, Critical, Durability *int
}

func (p ProfilePatch) Apply(profile Profile) (Profile, error) {
	if p.Address != nil {
		profile.Address = strings.TrimSpace(*p.Address)
	}
	if p.HighScore != nil {
		profile.HighScore = *p.HighScore
	}
	if p.Energy != nil {
		profile.Energy = *p.Energy
	}
	if p.Scenario != nil {
		profile.Scenario = *p.Scenario
	}
	if p.Head != nil {
		profile.Head = *p.Head
	}
	if p.Body != nil {
		profile.Body = *p.Body
	}
	if p.Arm != nil {
		profile.Arm = *p.Arm
	}
	if p.Health != nil {
		profile.Health = *p.Health
	}
	if p.Attack != nil {
		profile.Attack = *p.Attack
	}
	if p.Critical != nil {
		profile.Critical = *p.Critical
	}
	if p.Durability != nil {
		profile.Durability = *p.Durability
	}
	if !profile.Valid() {
		return Profile{}, ErrInvalidInput
	}
	return profile, nil
}

func (p Profile) Valid() bool {
	return p.HighScore >= 0 && p.Energy >= 0 && p.Energy <= 10000 && p.Scenario >= 0 && p.Head >= 0 && p.Body >= 0 && p.Arm >= 0 && p.Health >= 0 && p.Health <= 100000 && p.Attack >= 0 && p.Attack <= 100000 && p.Critical >= 0 && p.Critical <= 10000 && p.Durability >= 0 && p.Durability <= 100000 && len([]rune(p.Address)) <= 255
}

func CanonicalFriendPair(first, second int64) (int64, int64, error) {
	if first < 1 || second < 1 || first == second {
		return 0, 0, ErrInvalidInput
	}
	if first < second {
		return first, second, nil
	}
	return second, first, nil
}

const (
	BaseRateFactor     = 0.95
	RelativeRateFactor = 0.10
)

var DefaultCurrencyRates = map[string]int{
	"CNY": 177,
	"JPY": 1090,
	"KRW": 1335,
	"SPD": 1,
	"XAG": 27,
	"XPT": 1201,
}

func AdjustedRate(current int, usage, totalUsage int64) int {
	if current < 1 {
		return 1
	}
	if totalUsage <= 0 {
		return current
	}
	relative := 1 - float64(usage)/float64(totalUsage)
	return int(math.Max(1, math.Round(float64(current)*(BaseRateFactor+relative*RelativeRateFactor))))
}
