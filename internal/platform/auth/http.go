package auth

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"

	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

type Handler struct {
	identity *Service
	sessions *SessionService
	users    UserRepository
}

func NewHandler(identity *Service, sessions *SessionService, userRepository UserRepository) *Handler {
	return &Handler{identity: identity, sessions: sessions, users: userRepository}
}

func (h *Handler) RegisterRoutes(router *httpapi.Router, middleware *Middleware) {
	router.Handle(http.MethodPost, "/api/v1/auth/register", http.HandlerFunc(h.register))
	router.Handle(http.MethodPost, "/api/v1/auth/login", http.HandlerFunc(h.login))
	router.Handle(http.MethodPost, "/api/v1/auth/refresh", http.HandlerFunc(h.refresh))
	router.Handle(http.MethodPost, "/api/v1/auth/logout", http.HandlerFunc(h.logout))
	router.Handle(http.MethodGet, "/api/v1/me", middleware.Authenticate(http.HandlerFunc(h.me)))
}

func (h *Handler) register(w http.ResponseWriter, request *http.Request) {
	var input struct {
		Account     string `json:"account"`
		Email       string `json:"email"`
		Password    string `json:"password"`
		DisplayName string `json:"displayName"`
	}
	if !decodeJSON(w, request, &input) {
		return
	}
	user, err := h.identity.Register(request.Context(), RegisterRequest(input))
	if err != nil {
		writeAuthError(w, err)
		return
	}
	bundle, err := h.sessions.Start(request.Context(), user)
	if err != nil {
		writeAuthError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusCreated, sessionResponse(bundle))
}

func (h *Handler) login(w http.ResponseWriter, request *http.Request) {
	var input struct {
		Account  string `json:"account"`
		Password string `json:"password"`
	}
	if !decodeJSON(w, request, &input) {
		return
	}
	user, err := h.identity.Authenticate(request.Context(), input.Account, input.Password)
	if err != nil {
		writeAuthError(w, err)
		return
	}
	bundle, err := h.sessions.Start(request.Context(), user)
	if err != nil {
		writeAuthError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, sessionResponse(bundle))
}

func (h *Handler) refresh(w http.ResponseWriter, request *http.Request) {
	var input struct {
		RefreshToken string `json:"refreshToken"`
	}
	if !decodeJSON(w, request, &input) {
		return
	}
	bundle, err := h.sessions.Refresh(request.Context(), input.RefreshToken)
	if err != nil {
		writeAuthError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, sessionResponse(bundle))
}

func (h *Handler) logout(w http.ResponseWriter, request *http.Request) {
	var input struct {
		RefreshToken string `json:"refreshToken"`
	}
	if !decodeJSON(w, request, &input) {
		return
	}
	if err := h.sessions.Logout(request.Context(), input.RefreshToken); err != nil {
		writeAuthError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, map[string]bool{"loggedOut": true})
}

func (h *Handler) me(w http.ResponseWriter, request *http.Request) {
	subject, ok := SubjectFromContext(request.Context())
	if !ok {
		httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_REQUIRED", "A valid access token is required.")
		return
	}
	user, err := h.users.FindByID(request.Context(), subject.UserID)
	if err != nil {
		if errors.Is(err, users.ErrNotFound) {
			httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_INVALID_TOKEN", "The access token subject no longer exists.")
			return
		}
		writeAuthError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, userResponse(user))
}

func decodeJSON(w http.ResponseWriter, request *http.Request, destination any) bool {
	decoder := json.NewDecoder(request.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(destination); err != nil {
		httpapi.WriteError(w, http.StatusBadRequest, "INVALID_REQUEST", "The request body is invalid.")
		return false
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		httpapi.WriteError(w, http.StatusBadRequest, "INVALID_REQUEST", "The request body must contain one JSON object.")
		return false
	}
	return true
}

func writeAuthError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, ErrInvalidInput):
		httpapi.WriteError(w, http.StatusBadRequest, "AUTH_INVALID_INPUT", "The account details are invalid.")
	case errors.Is(err, ErrAlreadyExists):
		httpapi.WriteError(w, http.StatusConflict, "AUTH_ALREADY_EXISTS", "The account or email already exists.")
	case errors.Is(err, ErrInvalidCredentials):
		httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_INVALID_CREDENTIALS", "The account or password is invalid.")
	case errors.Is(err, ErrAccountSuspended):
		httpapi.WriteError(w, http.StatusForbidden, "AUTH_ACCOUNT_SUSPENDED", "The account is suspended.")
	case errors.Is(err, ErrInvalidRefreshToken):
		httpapi.WriteError(w, http.StatusUnauthorized, "AUTH_INVALID_REFRESH_TOKEN", "The refresh token is invalid or expired.")
	default:
		httpapi.WriteError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "An internal error occurred.")
	}
}

func sessionResponse(bundle *SessionBundle) map[string]any {
	return map[string]any{
		"accessToken": bundle.AccessToken, "refreshToken": bundle.RefreshToken,
		"accessExpiresAt": bundle.AccessExpiresAt, "refreshExpiresAt": bundle.RefreshExpiresAt,
		"user": userResponse(bundle.User),
	}
}

func userResponse(user *users.User) map[string]any {
	return map[string]any{
		"id": user.ID, "account": user.Account, "email": user.Email, "displayName": user.DisplayName,
		"role": user.Role, "status": user.Status, "createdAt": user.CreatedAt, "updatedAt": user.UpdatedAt,
	}
}
