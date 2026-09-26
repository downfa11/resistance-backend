package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/downfa11/resistance-backend/internal/platform/config"
	"github.com/downfa11/resistance-backend/internal/platform/server"
	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	cfg, err := config.Load(os.Getenv)
	if err != nil {
		log.Fatal(err)
	}

	db, err := platformsqlite.Open(ctx, cfg.DBPath)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := db.Close(); err != nil {
			log.Printf("close database: %v", err)
		}
	}()
	if err := platformsqlite.Migrate(ctx, db); err != nil {
		log.Fatal(err)
	}

	app, err := server.New(cfg, db)
	if err != nil {
		log.Fatal(err)
	}
	app.StartMaintenance(ctx)
	log.Printf("resistance-server listening environment=%s addr=%s database=%s", cfg.Environment, cfg.Addr, cfg.DBPath)
	if err := server.Serve(ctx, cfg.Addr, app.Handler()); err != nil {
		log.Fatal(err)
	}
}
