package migrations

import "embed"

// Files contains every ordered Arcade Server database migration.
//
//go:embed *.sql
var Files embed.FS
