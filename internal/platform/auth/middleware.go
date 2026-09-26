package auth

import (
	"context"
	"net/http"
	"strings"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

type subjectKey struct{}

type Middleware struct {
	tokens *TokenManager
	now    func() time.Time
}

func NewMiddleware(tokens *TokenManager) *Middleware {
	return &Middleware{tokens: tokens, now: time.Now}
}

func (m *Middleware) Authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, request *http.Request) {
		header := strings.TrimSpace(request.Header.Get("Authorization"))
		parts := strings.Fields(header)
		if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
			httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_REQUIRED", "A valid access token is required.")
			return
		}
		subject, err := m.tokens.Parse(parts[1], m.now().UTC())
		if err != nil {
			httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_INVALID_TOKEN", "The access token is invalid or expired.")
			return
		}
		ctx := context.WithValue(request.Context(), subjectKey{}, subject)
		next.ServeHTTP(w, request.WithContext(ctx))
	})
}

func (m *Middleware) RequireRole(role users.Role, next http.Handler) http.Handler {
	return m.Authenticate(http.HandlerFunc(func(w http.ResponseWriter, request *http.Request) {
		subject, ok := SubjectFromContext(request.Context())
		if !ok || subject.Role != role {
			httpapi.WriteError(w, http.StatusForbidden, "AUTH_FORBIDDEN", "The authenticated user cannot access this resource.")
			return
		}
		next.ServeHTTP(w, request)
	}))
}

func SubjectFromContext(ctx context.Context) (Subject, bool) {
	subject, ok := ctx.Value(subjectKey{}).(Subject)
	return subject, ok
}
