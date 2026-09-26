package auth

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"testing"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestAuthenticationHTTPLifecycle(t *testing.T) {
	t.Parallel()

	handler := newTestAuthHandler(t)
	registered := performJSON(t, handler, http.MethodPost, "/api/v1/auth/register", map[string]any{
		"account": "player.one", "email": "player@example.com", "password": "long-password", "displayName": "Player One",
	}, "")
	if registered.Code != http.StatusCreated {
		t.Fatalf("register status = %d, body = %s", registered.Code, registered.Body.String())
	}
	first := decodeSessionResponse(t, registered)
	if first.Data.AccessToken == "" || first.Data.RefreshToken == "" || first.Data.User.Password != "" {
		t.Fatalf("register response = %#v", first)
	}

	me := performJSON(t, handler, http.MethodGet, "/api/v1/me", nil, first.Data.AccessToken)
	if me.Code != http.StatusOK {
		t.Fatalf("me status = %d, body = %s", me.Code, me.Body.String())
	}
	loggedIn := performJSON(t, handler, http.MethodPost, "/api/v1/auth/login", map[string]any{"account": "player.one", "password": "long-password"}, "")
	if loggedIn.Code != http.StatusOK {
		t.Fatalf("login status = %d, body = %s", loggedIn.Code, loggedIn.Body.String())
	}
	loginSession := decodeSessionResponse(t, loggedIn)
	loggedOut := performJSON(t, handler, http.MethodPost, "/api/v1/auth/logout", map[string]any{"refreshToken": loginSession.Data.RefreshToken}, "")
	if loggedOut.Code != http.StatusOK {
		t.Fatalf("logout status = %d, body = %s", loggedOut.Code, loggedOut.Body.String())
	}
	refreshAfterLogout := performJSON(t, handler, http.MethodPost, "/api/v1/auth/refresh", map[string]any{"refreshToken": loginSession.Data.RefreshToken}, "")
	if refreshAfterLogout.Code != http.StatusUnauthorized {
		t.Fatalf("refresh after logout status = %d, body = %s", refreshAfterLogout.Code, refreshAfterLogout.Body.String())
	}

	refreshed := performJSON(t, handler, http.MethodPost, "/api/v1/auth/refresh", map[string]any{"refreshToken": first.Data.RefreshToken}, "")
	if refreshed.Code != http.StatusOK {
		t.Fatalf("refresh status = %d, body = %s", refreshed.Code, refreshed.Body.String())
	}
	second := decodeSessionResponse(t, refreshed)
	if second.Data.RefreshToken == first.Data.RefreshToken {
		t.Fatal("refresh token did not rotate")
	}

	reused := performJSON(t, handler, http.MethodPost, "/api/v1/auth/refresh", map[string]any{"refreshToken": first.Data.RefreshToken}, "")
	if reused.Code != http.StatusUnauthorized {
		t.Fatalf("reused refresh status = %d, body = %s", reused.Code, reused.Body.String())
	}
	revokedReplacement := performJSON(t, handler, http.MethodPost, "/api/v1/auth/refresh", map[string]any{"refreshToken": second.Data.RefreshToken}, "")
	if revokedReplacement.Code != http.StatusUnauthorized {
		t.Fatalf("replacement refresh status = %d, body = %s", revokedReplacement.Code, revokedReplacement.Body.String())
	}
}

func TestAuthenticationHTTPRejectsBadInputAndCredentials(t *testing.T) {
	t.Parallel()

	handler := newTestAuthHandler(t)
	unknownField := performJSON(t, handler, http.MethodPost, "/api/v1/auth/register", map[string]any{
		"account": "player", "email": "player@example.com", "password": "long-password", "displayName": "Player", "admin": true,
	}, "")
	if unknownField.Code != http.StatusBadRequest {
		t.Fatalf("unknown field status = %d", unknownField.Code)
	}
	badLogin := performJSON(t, handler, http.MethodPost, "/api/v1/auth/login", map[string]any{"account": "missing", "password": "long-password"}, "")
	if badLogin.Code != http.StatusUnauthorized {
		t.Fatalf("bad login status = %d, body = %s", badLogin.Code, badLogin.Body.String())
	}
	missingBearer := performJSON(t, handler, http.MethodGet, "/api/v1/me", nil, "")
	if missingBearer.Code != http.StatusUnauthorized {
		t.Fatalf("missing bearer status = %d", missingBearer.Code)
	}
}

type sessionEnvelope struct {
	Data struct {
		AccessToken  string `json:"accessToken"`
		RefreshToken string `json:"refreshToken"`
		User         struct {
			ID       int64  `json:"id"`
			Password string `json:"password"`
		} `json:"user"`
	} `json:"data"`
}

func newTestAuthHandler(t *testing.T) http.Handler {
	t.Helper()
	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	userRepository := users.NewRepository(db)
	passwords := NewPasswordHasher(PasswordParams{Memory: 8 * 1024, Iterations: 1, Parallelism: 1, SaltLength: 16, KeyLength: 16})
	identity := NewService(userRepository, passwords)
	tokens, err := NewTokenManager("test-signing-secret", 15*time.Minute)
	if err != nil {
		t.Fatalf("create token manager: %v", err)
	}
	sessions := NewSessionService(NewSessionRepository(db), userRepository, tokens, 30*24*time.Hour)
	authHandler := NewHandler(identity, sessions, userRepository)
	router := httpapi.NewRouter()
	authHandler.RegisterRoutes(router, NewMiddleware(tokens))
	return httpapi.Recover(httpapi.LimitBody(httpapi.DefaultBodyLimit, router))
}

func performJSON(t *testing.T, handler http.Handler, method, path string, body any, accessToken string) *httptest.ResponseRecorder {
	t.Helper()
	var encoded []byte
	if body != nil {
		var err error
		encoded, err = json.Marshal(body)
		if err != nil {
			t.Fatalf("encode request: %v", err)
		}
	}
	request := httptest.NewRequest(method, path, bytes.NewReader(encoded))
	if body != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	if accessToken != "" {
		request.Header.Set("Authorization", "Bearer "+accessToken)
	}
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, request)
	return recorder
}

func decodeSessionResponse(t *testing.T, recorder *httptest.ResponseRecorder) sessionEnvelope {
	t.Helper()
	var response sessionEnvelope
	if err := json.Unmarshal(recorder.Body.Bytes(), &response); err != nil {
		t.Fatalf("decode session response: %v", err)
	}
	return response
}
