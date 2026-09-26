package sqlite

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/downfa11/resistance-backend/migrations"
	"github.com/pressly/goose/v3"
)

func Migrate(ctx context.Context, db *sql.DB) error {
	if db == nil {
		return fmt.Errorf("migrate sqlite: database is required")
	}

	provider, err := goose.NewProvider(goose.DialectSQLite3, db, migrations.Files)
	if err != nil {
		return fmt.Errorf("create migration provider: %w", err)
	}
	if _, err := provider.Up(ctx); err != nil {
		return fmt.Errorf("apply migrations: %w", err)
	}
	return nil
}
