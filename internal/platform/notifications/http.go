package notifications

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/downfa11/resistance-backend/internal/platform/auth"
	"github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/platform/users"
)

type Handler struct {
	service *Service
}

func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

func (h *Handler) RegisterRoutes(router *httpapi.Router, middleware *auth.Middleware) {
	router.Handle(http.MethodGet, "/api/v1/notifications", middleware.Authenticate(http.HandlerFunc(h.list)))
	router.Handle(http.MethodGet, "/api/v1/notifications/{id}", middleware.Authenticate(http.HandlerFunc(h.find)))
	router.Handle(http.MethodPost, "/api/v1/notifications/{id}/read", middleware.Authenticate(http.HandlerFunc(h.markRead)))
	router.Handle(http.MethodPost, "/api/v1/notifications/read-all", middleware.Authenticate(http.HandlerFunc(h.markAllRead)))
	router.Handle(http.MethodPost, "/api/v1/admin/notifications", middleware.RequireRole(users.RoleAdmin, http.HandlerFunc(h.adminCreate)))
}

func (h *Handler) list(w http.ResponseWriter, request *http.Request) {
	subject, _ := auth.SubjectFromContext(request.Context())
	limit, err := queryInteger(request, "limit", 20)
	if err != nil {
		writeError(w, ErrInvalidInput)
		return
	}
	beforeID, err := queryInteger64(request, "beforeId", 0)
	if err != nil {
		writeError(w, ErrInvalidInput)
		return
	}
	items, err := h.service.List(request.Context(), subject.UserID, limit, beforeID)
	if err != nil {
		writeError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, map[string]any{"items": notificationResponses(items), "nextBeforeId": nextBeforeID(items)})
}

func (h *Handler) find(w http.ResponseWriter, request *http.Request) {
	subject, _ := auth.SubjectFromContext(request.Context())
	id, err := positivePathID(request)
	if err != nil {
		writeError(w, ErrInvalidInput)
		return
	}
	item, err := h.service.Find(request.Context(), subject.UserID, id)
	if err != nil {
		writeError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, notificationResponse(item))
}

func (h *Handler) markRead(w http.ResponseWriter, request *http.Request) {
	subject, _ := auth.SubjectFromContext(request.Context())
	id, err := positivePathID(request)
	if err != nil {
		writeError(w, ErrInvalidInput)
		return
	}
	item, err := h.service.MarkRead(request.Context(), subject.UserID, id)
	if err != nil {
		writeError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, notificationResponse(item))
}

func (h *Handler) markAllRead(w http.ResponseWriter, request *http.Request) {
	subject, _ := auth.SubjectFromContext(request.Context())
	count, err := h.service.MarkAllRead(request.Context(), subject.UserID)
	if err != nil {
		writeError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusOK, map[string]int64{"updated": count})
}

func (h *Handler) adminCreate(w http.ResponseWriter, request *http.Request) {
	var input struct {
		UserID    int64      `json:"userId"`
		Source    Source     `json:"source"`
		Title     string     `json:"title"`
		Body      string     `json:"body"`
		DeepLink  string     `json:"deepLink"`
		ExpiresAt *time.Time `json:"expiresAt"`
	}
	if !decodeJSON(w, request, &input) {
		return
	}
	item, err := h.service.Create(request.Context(), CreateRequest(input))
	if err != nil {
		writeError(w, err)
		return
	}
	httpapi.WriteData(w, http.StatusCreated, notificationResponse(item))
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

func positivePathID(request *http.Request) (int64, error) {
	id, err := strconv.ParseInt(request.PathValue("id"), 10, 64)
	if err != nil || id < 1 {
		return 0, ErrInvalidInput
	}
	return id, nil
}

func queryInteger(request *http.Request, key string, fallback int) (int, error) {
	value := request.URL.Query().Get(key)
	if value == "" {
		return fallback, nil
	}
	parsed, err := strconv.Atoi(value)
	return parsed, err
}

func queryInteger64(request *http.Request, key string, fallback int64) (int64, error) {
	value := request.URL.Query().Get(key)
	if value == "" {
		return fallback, nil
	}
	parsed, err := strconv.ParseInt(value, 10, 64)
	return parsed, err
}

func writeError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, ErrInvalidInput):
		httpapi.WriteError(w, http.StatusBadRequest, "NOTIFICATION_INVALID_INPUT", "The notification request is invalid.")
	case errors.Is(err, ErrNotFound):
		httpapi.WriteError(w, http.StatusNotFound, "NOTIFICATION_NOT_FOUND", "The notification was not found.")
	default:
		httpapi.WriteError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "An internal error occurred.")
	}
}

func notificationResponses(items []*Notification) []map[string]any {
	responses := make([]map[string]any, 0, len(items))
	for _, item := range items {
		responses = append(responses, notificationResponse(item))
	}
	return responses
}

func notificationResponse(item *Notification) map[string]any {
	return map[string]any{
		"id": item.ID, "source": item.Source, "title": item.Title, "body": item.Body,
		"deepLink": item.DeepLink, "readAt": item.ReadAt, "expiresAt": item.ExpiresAt, "createdAt": item.CreatedAt,
	}
}

func nextBeforeID(items []*Notification) int64 {
	if len(items) == 0 {
		return 0
	}
	return items[len(items)-1].ID
}
