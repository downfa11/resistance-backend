package notifications

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"strconv"
	"testing"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/auth"
	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	platformsqlite "github.com/downfa11/resistance-backend/internal/platform/sqlite"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

func TestNotificationHTTPAdminSendAndRecipientInbox(t *testing.T) {
	t.Parallel()

	handler, player, admin := newNotificationHTTPTest(t)
	created := notificationRequest(t, handler, http.MethodPost, "/api/v1/admin/notifications", map[string]any{
		"userId": player.userID, "source": "RESISTANCE", "title": "Friend request", "body": "A friend request arrived.", "deepLink": "/friends",
	}, admin.token)
	if created.Code != http.StatusCreated {
		t.Fatalf("admin create status = %d, body = %s", created.Code, created.Body.String())
	}

	listed := notificationRequest(t, handler, http.MethodGet, "/api/v1/notifications?limit=20", nil, player.token)
	if listed.Code != http.StatusOK {
		t.Fatalf("list status = %d, body = %s", listed.Code, listed.Body.String())
	}
	var listResponse struct {
		Data struct {
			Items []struct {
				ID     int64  `json:"id"`
				Source string `json:"source"`
				ReadAt any    `json:"readAt"`
			} `json:"items"`
		} `json:"data"`
	}
	if err := json.Unmarshal(listed.Body.Bytes(), &listResponse); err != nil {
		t.Fatalf("decode list: %v", err)
	}
	if len(listResponse.Data.Items) != 1 || listResponse.Data.Items[0].Source != "RESISTANCE" {
		t.Fatalf("list response = %#v", listResponse)
	}
	id := listResponse.Data.Items[0].ID
	read := notificationRequest(t, handler, http.MethodPost, "/api/v1/notifications/"+strconv.FormatInt(id, 10)+"/read", nil, player.token)
	if read.Code != http.StatusOK {
		t.Fatalf("read status = %d, body = %s", read.Code, read.Body.String())
	}

	notOwned := notificationRequest(t, handler, http.MethodGet, "/api/v1/notifications/"+strconv.FormatInt(id, 10), nil, admin.token)
	if notOwned.Code != http.StatusNotFound {
		t.Fatalf("cross-user get status = %d, body = %s", notOwned.Code, notOwned.Body.String())
	}
}

func TestNotificationHTTPRequiresAuthenticationAndAdminRole(t *testing.T) {
	t.Parallel()

	handler, player, _ := newNotificationHTTPTest(t)
	unauthenticated := notificationRequest(t, handler, http.MethodGet, "/api/v1/notifications", nil, "")
	if unauthenticated.Code != http.StatusUnauthorized {
		t.Fatalf("unauthenticated status = %d", unauthenticated.Code)
	}
	forbidden := notificationRequest(t, handler, http.MethodPost, "/api/v1/admin/notifications", map[string]any{
		"userId": player.userID, "source": "SYSTEM", "title": "Title", "body": "Body",
	}, player.token)
	if forbidden.Code != http.StatusForbidden {
		t.Fatalf("forbidden status = %d, body = %s", forbidden.Code, forbidden.Body.String())
	}
}

type authenticatedUser struct {
	userID int64
	token  string
}

func newNotificationHTTPTest(t *testing.T) (http.Handler, authenticatedUser, authenticatedUser) {
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
	now := time.Now().UTC()
	createUser := func(account string, role users.Role) authenticatedUser {
		user := &users.User{Account: account, Email: account + "@example.com", PasswordHash: "hash", DisplayName: account, Role: role, Status: users.StatusActive, CreatedAt: now, UpdatedAt: now}
		if err := userRepository.Create(context.Background(), user); err != nil {
			t.Fatalf("create user: %v", err)
		}
		tokens, err := auth.NewTokenManager("test-signing-secret", time.Hour)
		if err != nil {
			t.Fatalf("create token manager: %v", err)
		}
		token, err := tokens.Issue(user.ID, user.Role, "session-"+account, now)
		if err != nil {
			t.Fatalf("issue token: %v", err)
		}
		return authenticatedUser{userID: user.ID, token: token}
	}
	tokens, err := auth.NewTokenManager("test-signing-secret", time.Hour)
	if err != nil {
		t.Fatalf("create middleware token manager: %v", err)
	}
	router := httpapi.NewRouter()
	NewHandler(NewService(NewRepository(db))).RegisterRoutes(router, auth.NewMiddleware(tokens))
	return router, createUser("player", users.RolePlayer), createUser("admin", users.RoleAdmin)
}

func notificationRequest(t *testing.T, handler http.Handler, method, path string, body any, token string) *httptest.ResponseRecorder {
	t.Helper()
	var encoded []byte
	if body != nil {
		var err error
		encoded, err = json.Marshal(body)
		if err != nil {
			t.Fatalf("encode body: %v", err)
		}
	}
	request := httptest.NewRequest(method, path, bytes.NewReader(encoded))
	if token != "" {
		request.Header.Set("Authorization", "Bearer "+token)
	}
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, request)
	return recorder
}
