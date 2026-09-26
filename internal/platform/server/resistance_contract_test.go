package server

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"testing"

	"github.com/downfa11/resistance-backend/internal/platform/config"
	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
)

func TestResistanceUnityContractUsesNumericAppearanceAndAuthoritativeBalances(t *testing.T) {
	db, err := platformsqlite.Open(context.Background(), filepath.Join(t.TempDir(), "resistance.db"))
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() { _ = db.Close() })
	if err := platformsqlite.Migrate(context.Background(), db); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	app, err := New(config.Config{JWTSecret: "test-signing-secret"}, db)
	if err != nil {
		t.Fatalf("create app: %v", err)
	}

	registration := requestJSON(t, app.Handler(), http.MethodPost, "/api/v1/auth/register", "", map[string]any{
		"account": "resistance-player", "email": "resistance@example.com", "password": "correct horse battery staple", "displayName": "Resistance Player",
	})
	var session struct {
		Data struct {
			AccessToken string `json:"accessToken"`
			User        struct {
				ID int64 `json:"id"`
			} `json:"user"`
		} `json:"data"`
	}
	if err := json.Unmarshal(registration.Body.Bytes(), &session); err != nil || session.Data.AccessToken == "" {
		t.Fatalf("decode registration: %v, body = %s", err, registration.Body.String())
	}

	created := requestJSON(t, app.Handler(), http.MethodPut, "/api/v1/resistance/me/profile", session.Data.AccessToken, map[string]any{"address": "Seoul"})
	if created.Code != http.StatusOK {
		t.Fatalf("create profile status = %d, body = %s", created.Code, created.Body.String())
	}
	patched := requestJSON(t, app.Handler(), http.MethodPatch, "/api/v1/resistance/me/profile", session.Data.AccessToken, map[string]any{
		"scenario": 6, "head": 2, "body": 3, "arm": 4, "highScore": 99,
	})
	if patched.Code != http.StatusOK {
		t.Fatalf("patch profile status = %d, body = %s", patched.Code, patched.Body.String())
	}
	var profile struct {
		Data struct {
			DisplayName string `json:"displayName"`
			Scenario    int    `json:"scenario"`
			Head        int    `json:"head"`
			Body        int    `json:"body"`
			Arm         int    `json:"arm"`
		} `json:"data"`
	}
	if err := json.Unmarshal(patched.Body.Bytes(), &profile); err != nil {
		t.Fatalf("decode profile: %v", err)
	}
	if profile.Data.DisplayName != "Resistance Player" || profile.Data.Scenario != 6 || profile.Data.Head != 2 || profile.Data.Body != 3 || profile.Data.Arm != 4 {
		t.Fatalf("unexpected Unity profile DTO: %+v", profile.Data)
	}

	if _, err := db.Exec(`INSERT INTO resistance_currency_balances (user_id, code, quantity, updated_at) VALUES (?, 'JPY', 3, '2026-09-26T00:00:00Z')`, session.Data.User.ID); err != nil {
		t.Fatalf("seed balance: %v", err)
	}
	exchange := requestJSONWithHeaders(t, app.Handler(), http.MethodPost, "/api/v1/resistance/exchanges", session.Data.AccessToken, map[string]any{"currencyCode": "JPY", "quantity": 2}, map[string]string{"Idempotency-Key": "unity-exchange-1"})
	if exchange.Code != http.StatusCreated {
		t.Fatalf("exchange status = %d, body = %s", exchange.Code, exchange.Body.String())
	}
	var result struct {
		Data struct {
			CurrencyCode string `json:"currencyCode"`
			Balance      int64  `json:"balance"`
			Gold         int64  `json:"gold"`
		} `json:"data"`
	}
	if err := json.Unmarshal(exchange.Body.Bytes(), &result); err != nil {
		t.Fatalf("decode exchange: %v", err)
	}
	if result.Data.CurrencyCode != "JPY" || result.Data.Balance != 1 || result.Data.Gold != 2180 {
		t.Fatalf("unexpected authoritative exchange state: %+v", result.Data)
	}
}

func requestJSONWithHeaders(t *testing.T, handler http.Handler, method, path, token string, body any, headers map[string]string) *httptest.ResponseRecorder {
	t.Helper()
	recorder := requestJSON(t, http.HandlerFunc(func(w http.ResponseWriter, request *http.Request) {
		for key, value := range headers {
			request.Header.Set(key, value)
		}
		handler.ServeHTTP(w, request)
	}), method, path, token, body)
	return recorder
}
