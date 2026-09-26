package migrations

import "embed"

// Files contains every ordered Resistance Server database migration.
//
//go:embed *.sql
var Files embed.FS
