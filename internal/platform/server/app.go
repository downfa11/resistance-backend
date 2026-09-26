package server

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/auth"
	"github.com/downfa11/resistance-backend/internal/platform/config"
	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/platform/notifications"
	"github.com/downfa11/resistance-backend/internal/platform/users"
	resistancehttp "github.com/downfa11/resistance-backend/internal/resistance/httpapi"
	resistanceservice "github.com/downfa11/resistance-backend/internal/resistance/service"
)

const (
	readinessTimeout       = time.Second
	accessTokenTTL         = 15 * time.Minute
	refreshTokenTTL        = 30 * 24 * time.Hour
	sessionCleanupInterval = time.Hour
	sessionCleanupBatch    = 1000
)

type App struct {
	db       *sql.DB
	handler  http.Handler
	sessions *auth.SessionRepository
}

func New(cfg config.Config, db *sql.DB) (*App, error) {
	if db == nil {
		return nil, fmt.Errorf("create server: database is required")
	}

	app := &App{db: db, sessions: auth.NewSessionRepository(db)}
	router := httpapi.NewRouter()
	userRepository := users.NewRepository(db)
	identityService := auth.NewService(userRepository, auth.NewPasswordHasher(auth.DefaultPasswordParams()))
	if cfg.BootstrapAdminAccount != "" {
		bootstrapCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if _, err := identityService.BootstrapAdministrator(bootstrapCtx, auth.RegisterRequest{
			Account: cfg.BootstrapAdminAccount, Email: cfg.BootstrapAdminEmail,
			Password: cfg.BootstrapAdminPassword, DisplayName: cfg.BootstrapAdminDisplayName,
		}); err != nil {
			return nil, fmt.Errorf("bootstrap administrator: %w", err)
		}
	}
	tokenManager, err := auth.NewTokenManager(cfg.JWTSecret, accessTokenTTL)
	if err != nil {
		return nil, err
	}
	sessionService := auth.NewSessionService(app.sessions, userRepository, tokenManager, refreshTokenTTL)
	authMiddleware := auth.NewMiddleware(tokenManager)
	auth.NewHandler(identityService, sessionService, userRepository).RegisterRoutes(router, authMiddleware)
	notificationService := notifications.NewService(notifications.NewRepository(db))
	notifications.NewHandler(notificationService).RegisterRoutes(router, authMiddleware)
	resistancehttp.NewHandler(resistanceservice.New(db)).RegisterRoutes(router, authMiddleware)
	router.Handle(http.MethodGet, "/healthz", http.HandlerFunc(app.health))
	router.Handle(http.MethodGet, "/readyz", http.HandlerFunc(app.ready))
	app.handler = httpapi.RequestID(
		httpapi.Recover(
			httpapi.CORS(cfg.AllowedOrigins,
				httpapi.LimitBody(httpapi.DefaultBodyLimit, router),
			),
		),
	)
	return app, nil
}

func (a *App) StartMaintenance(ctx context.Context) {
	go func() {
		a.cleanupExpiredSessions(ctx)
		ticker := time.NewTicker(sessionCleanupInterval)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				a.cleanupExpiredSessions(ctx)
			}
		}
	}()
}

func (a *App) cleanupExpiredSessions(ctx context.Context) {
	for {
		deleted, err := a.sessions.DeleteExpiredFamilies(ctx, time.Now().UTC(), sessionCleanupBatch)
		if err != nil {
			if ctx.Err() == nil {
				log.Printf("cleanup expired refresh sessions: %v", err)
			}
			return
		}
		if deleted < sessionCleanupBatch {
			return
		}
	}
}

func (a *App) Handler() http.Handler {
	return a.handler
}

func (a *App) health(w http.ResponseWriter, _ *http.Request) {
	httpapi.WriteData(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (a *App) ready(w http.ResponseWriter, request *http.Request) {
	ctx, cancel := context.WithTimeout(request.Context(), readinessTimeout)
	defer cancel()
	if err := a.db.PingContext(ctx); err != nil {
		httpapi.WriteError(w, http.StatusServiceUnavailable, "SERVICE_UNAVAILABLE", "The service is not ready.")
		return
	}
	httpapi.WriteData(w, http.StatusOK, map[string]string{"status": "ready"})
}
