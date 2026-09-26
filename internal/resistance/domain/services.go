package domain

import "time"

type CurrencyRate struct {
	Code      string
	Rate      int
	Uses      int64
	UpdatedAt time.Time
}
type CurrencyBalance struct {
	Code     string
	Quantity int64
}
type Exchange struct {
	ID, UserID     int64
	CurrencyCode   string
	Quantity       int64
	Rate           int
	GoldGranted    int64
	BalanceAfter   int64
	GoldAfter      int64
	IdempotencyKey string
	CreatedAt      time.Time
}
type SupporterCode struct {
	ID         int64
	Kind, Code string
	RewardGold int64
	Status     string
	RedeemedBy *int64
	RedeemedAt *time.Time
	CreatedAt  time.Time
}
type SupporterDetail struct {
	ID                   int64
	Title, Details       string
	CreatedAt, UpdatedAt time.Time
}
type Notice struct {
	ID              int64
	Title, Body     string
	PublishedAt     *time.Time
	CreatedByUserID int64
	CreatedAt       time.Time
}
type ContentEntry struct {
	ChapterID, ID, MediaType, Body, SHA256 string
	Ordinal                                int
}
type ContentManifest struct {
	Version     string
	PublishedAt time.Time
	Entries     []ContentEntry
}
