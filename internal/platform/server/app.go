package server

import (
	"context"
	"database/sql"
	"fmt"
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
	readinessTimeout = time.Second
	accessTokenTTL   = 15 * time.Minute
	refreshTokenTTL  = 30 * 24 * time.Hour
)

type App struct {
	db      *sql.DB
	handler http.Handler
}

func New(cfg config.Config, db *sql.DB) (*App, error) {
	if db == nil {
		return nil, fmt.Errorf("create server: database is required")
	}

	app := &App{db: db}
	router := httpapi.NewRouter()
	userRepository := users.NewRepository(db)
	identityService := auth.NewService(userRepository, auth.NewPasswordHasher(auth.DefaultPasswordParams()))
	tokenManager, err := auth.NewTokenManager(cfg.JWTSecret, accessTokenTTL)
	if err != nil {
		return nil, err
	}
	sessionService := auth.NewSessionService(auth.NewSessionRepository(db), userRepository, tokenManager, refreshTokenTTL)
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
