package httpapi

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"

	"github.com/downfa11/resistance-backend/internal/platform/auth"
	platformhttp "github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/platform/users"
	"github.com/downfa11/resistance-backend/internal/resistance/domain"
	"github.com/downfa11/resistance-backend/internal/resistance/repository"
	"github.com/downfa11/resistance-backend/internal/resistance/service"
)

type Handler struct{ service *service.Service }

func NewHandler(service *service.Service) *Handler { return &Handler{service: service} }

func (h *Handler) RegisterRoutes(router *platformhttp.Router, middleware *auth.Middleware) {
	router.Handle(http.MethodPut, "/api/v1/resistance/me/profile", middleware.Authenticate(http.HandlerFunc(h.ensureProfile)))
	router.Handle(http.MethodGet, "/api/v1/resistance/me/profile", middleware.Authenticate(http.HandlerFunc(h.profile)))
	router.Handle(http.MethodPatch, "/api/v1/resistance/me/profile", middleware.Authenticate(http.HandlerFunc(h.patchProfile)))
	router.Handle(http.MethodGet, "/api/v1/resistance/friends", middleware.Authenticate(http.HandlerFunc(h.friends)))
	router.Handle(http.MethodGet, "/api/v1/resistance/friends/requests", middleware.Authenticate(http.HandlerFunc(h.friendRequests)))
	router.Handle(http.MethodGet, "/api/v1/resistance/friends/suggestions", middleware.Authenticate(http.HandlerFunc(h.randomAllies)))
	router.Handle(http.MethodPost, "/api/v1/resistance/friends/requests", middleware.Authenticate(http.HandlerFunc(h.requestFriend)))
	router.Handle(http.MethodPost, "/api/v1/resistance/friends/requests/{id}/accept", middleware.Authenticate(http.HandlerFunc(h.acceptFriend)))
	router.Handle(http.MethodDelete, "/api/v1/resistance/friends/{id}", middleware.Authenticate(http.HandlerFunc(h.deleteFriend)))
	router.Handle(http.MethodGet, "/api/v1/resistance/exchange-rates", middleware.Authenticate(http.HandlerFunc(h.rates)))
	router.Handle(http.MethodGet, "/api/v1/resistance/me/balances", middleware.Authenticate(http.HandlerFunc(h.balances)))
	router.Handle(http.MethodPost, "/api/v1/resistance/exchanges", middleware.Authenticate(http.HandlerFunc(h.exchange)))
	router.Handle(http.MethodPost, "/api/v1/resistance/supporters/redemptions", middleware.Authenticate(http.HandlerFunc(h.redeemSupporter)))
	router.Handle(http.MethodGet, "/api/v1/resistance/supporters/details", middleware.Authenticate(http.HandlerFunc(h.supporterDetails)))
	router.Handle(http.MethodGet, "/api/v1/resistance/content/manifest", http.HandlerFunc(h.contentManifest))
	router.Handle(http.MethodGet, "/api/v1/resistance/content/items/{id...}", http.HandlerFunc(h.contentItem))
	router.Handle(http.MethodGet, "/api/v1/resistance/notices", middleware.Authenticate(http.HandlerFunc(h.notices)))
	router.Handle(http.MethodPut, "/api/v1/admin/resistance/balances", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.setBalance)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/exchange-rates/adjust", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.adjustRates)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/exchange-rates/reset", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.resetRates)))
	router.Handle(http.MethodGet, "/api/v1/admin/resistance/supporter-codes", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.supporterCodes)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/supporter-codes", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.createSupporterCode)))
	router.Handle(http.MethodDelete, "/api/v1/admin/resistance/supporter-codes/{id}", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.deleteSupporterCode)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/supporter-details", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.createSupporterDetail)))
	router.Handle(http.MethodPut, "/api/v1/admin/resistance/supporter-details/{id}", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.updateSupporterDetail)))
	router.Handle(http.MethodDelete, "/api/v1/admin/resistance/supporter-details/{id}", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.deleteSupporterDetail)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/notices", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.publishNotice)))
	router.Handle(http.MethodPost, "/api/v1/admin/resistance/content", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.publishContent)))
}

func (h *Handler) ensureProfile(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		Address string `json:"address"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.EnsureProfile(request.Context(), userID, input.Address)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, profileResponse(item))
}

func (h *Handler) profile(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	item, err := h.service.Profile(request.Context(), userID)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, profileResponse(item))
}

func (h *Handler) patchProfile(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		Address    *string `json:"address"`
		HighScore  *int    `json:"highScore"`
		Energy     *int    `json:"energy"`
		Scenario   *int    `json:"scenario"`
		Head       *int    `json:"head"`
		Body       *int    `json:"body"`
		Arm        *int    `json:"arm"`
		Health     *int    `json:"health"`
		Attack     *int    `json:"attack"`
		Critical   *int    `json:"critical"`
		Durability *int    `json:"durability"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.PatchProfile(request.Context(), userID, domain.ProfilePatch{Address: input.Address, HighScore: input.HighScore, Energy: input.Energy, Scenario: input.Scenario, Head: input.Head, Body: input.Body, Arm: input.Arm, Health: input.Health, Attack: input.Attack, Critical: input.Critical, Durability: input.Durability})
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, profileResponse(item))
}

func (h *Handler) friends(w http.ResponseWriter, request *http.Request) {
	h.allies(w, request, h.service.Friends)
}
func (h *Handler) friendRequests(w http.ResponseWriter, request *http.Request) {
	h.allies(w, request, h.service.FriendRequests)
}
func (h *Handler) randomAllies(w http.ResponseWriter, request *http.Request) {
	h.allies(w, request, h.service.RandomAllies)
}
func (h *Handler) allies(w http.ResponseWriter, request *http.Request, list func(context.Context, int64) ([]repository.Ally, error)) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	items, err := list(request.Context(), userID)
	if err != nil {
		writeError(w, err)
		return
	}
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, allyResponse(item))
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"items": result})
}

func (h *Handler) requestFriend(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		TargetUserID int64 `json:"targetUserId"`
	}
	if !decode(w, request, &input) {
		return
	}
	if err := h.service.RequestFriend(request.Context(), userID, input.TargetUserID); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, map[string]bool{"requested": true})
}

func (h *Handler) acceptFriend(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	id, ok := pathID(w, request)
	if !ok {
		return
	}
	if err := h.service.AcceptFriend(request.Context(), userID, id); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]bool{"accepted": true})
}

func (h *Handler) deleteFriend(w http.ResponseWriter, request *http.Request) {
	userID, ok := userID(w, request)
	if !ok {
		return
	}
	id, ok := pathID(w, request)
	if !ok {
		return
	}
	if err := h.service.DeleteFriend(request.Context(), userID, id); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]bool{"deleted": true})
}

func userID(w http.ResponseWriter, request *http.Request) (int64, bool) {
	subject, ok := auth.SubjectFromContext(request.Context())
	if !ok {
		platformhttp.WriteError(w, http.StatusUnauthorized, "AUTH_REQUIRED", "A valid access token is required.")
		return 0, false
	}
	return subject.UserID, true
}
func pathID(w http.ResponseWriter, request *http.Request) (int64, bool) {
	id, err := strconv.ParseInt(request.PathValue("id"), 10, 64)
	if err != nil || id < 1 {
		platformhttp.WriteError(w, http.StatusBadRequest, "RESISTANCE_INVALID_INPUT", "The Resistance request is invalid.")
		return 0, false
	}
	return id, true
}
func decode(w http.ResponseWriter, request *http.Request, value any) bool {
	decoder := json.NewDecoder(request.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(value); err != nil {
		platformhttp.WriteError(w, http.StatusBadRequest, "INVALID_REQUEST", "The request body is invalid.")
		return false
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		platformhttp.WriteError(w, http.StatusBadRequest, "INVALID_REQUEST", "The request body must contain one JSON object.")
		return false
	}
	return true
}
func writeError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, domain.ErrInvalidInput):
		platformhttp.WriteError(w, http.StatusBadRequest, "RESISTANCE_INVALID_INPUT", "The Resistance request is invalid.")
	case errors.Is(err, domain.ErrNotFound):
		platformhttp.WriteError(w, http.StatusNotFound, "RESISTANCE_NOT_FOUND", "The Resistance resource was not found.")
	case errors.Is(err, domain.ErrConflict), errors.Is(err, domain.ErrInsufficientCurrency), errors.Is(err, domain.ErrIdempotencyConflict):
		platformhttp.WriteError(w, http.StatusConflict, "RESISTANCE_CONFLICT", err.Error())
	default:
		platformhttp.WriteError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "An internal error occurred.")
	}
}
func profileResponse(item *domain.Profile) map[string]any {
	return map[string]any{"userId": item.UserID, "displayName": item.DisplayName, "address": item.Address, "gold": item.Gold, "highScore": item.HighScore, "energy": item.Energy, "scenario": item.Scenario, "head": item.Head, "body": item.Body, "arm": item.Arm, "health": item.Health, "attack": item.Attack, "critical": item.Critical, "durability": item.Durability, "createdAt": item.CreatedAt, "updatedAt": item.UpdatedAt}
}
func allyResponse(item repository.Ally) map[string]any {
	return map[string]any{"userId": item.UserID, "displayName": item.DisplayName, "highScore": item.HighScore, "head": item.Head, "body": item.Body, "arm": item.Arm, "health": item.Health, "attack": item.Attack, "critical": item.Critical, "durability": item.Durability}
}
