package httpapi

import (
	"net/http"
	"strconv"
	"strings"

	platformhttp "github.com/downfa11/resistance-backend/internal/platform/httpapi"
	"github.com/downfa11/resistance-backend/internal/resistance/domain"
)

func (h *Handler) rates(w http.ResponseWriter, request *http.Request) {
	items, err := h.service.Rates(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeRates(w, items)
}

func (h *Handler) adjustRates(w http.ResponseWriter, request *http.Request) {
	items, err := h.service.AdjustRates(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeRates(w, items)
}

func (h *Handler) resetRates(w http.ResponseWriter, request *http.Request) {
	items, err := h.service.ResetRates(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeRates(w, items)
}

func writeRates(w http.ResponseWriter, items []domain.CurrencyRate) {
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, map[string]any{"code": item.Code, "rate": item.Rate, "uses": item.Uses, "updatedAt": item.UpdatedAt})
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"exchangeRates": result})
}

func (h *Handler) balances(w http.ResponseWriter, request *http.Request) {
	id, ok := userID(w, request)
	if !ok {
		return
	}
	items, err := h.service.Balances(request.Context(), id)
	if err != nil {
		writeError(w, err)
		return
	}
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, map[string]any{"code": item.Code, "quantity": item.Quantity})
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"balances": result})
}

func (h *Handler) exchange(w http.ResponseWriter, request *http.Request) {
	id, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		CurrencyCode string `json:"currencyCode"`
		Quantity     int64  `json:"quantity"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.Exchange(request.Context(), id, input.CurrencyCode, input.Quantity, strings.TrimSpace(request.Header.Get("Idempotency-Key")))
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, exchangeResponse(item))
}

func (h *Handler) setBalance(w http.ResponseWriter, request *http.Request) {
	var input struct {
		UserID       int64  `json:"userId"`
		CurrencyCode string `json:"currencyCode"`
		Quantity     int64  `json:"quantity"`
	}
	if !decode(w, request, &input) {
		return
	}
	if err := h.service.SetBalance(request.Context(), input.UserID, input.CurrencyCode, input.Quantity); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]bool{"updated": true})
}

func (h *Handler) createSupporterCode(w http.ResponseWriter, request *http.Request) {
	var input struct {
		Kind       string `json:"kind"`
		Code       string `json:"code"`
		RewardGold int64  `json:"rewardGold"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.CreateSupporterCode(request.Context(), input.Kind, input.Code, input.RewardGold)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, map[string]any{"id": item.ID, "kind": item.Kind, "code": item.Code, "rewardGold": item.RewardGold, "status": item.Status, "createdAt": item.CreatedAt})
}

func (h *Handler) supporterCodes(w http.ResponseWriter, request *http.Request) {
	limit := 200
	if raw := strings.TrimSpace(request.URL.Query().Get("limit")); raw != "" {
		value, err := strconv.Atoi(raw)
		if err != nil {
			platformhttp.WriteError(w, http.StatusBadRequest, "RESISTANCE_INVALID_INPUT", "The Resistance request is invalid.")
			return
		}
		limit = value
	}
	items, err := h.service.SupporterCodes(request.Context(), limit)
	if err != nil {
		writeError(w, err)
		return
	}
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, supporterCodeResponse(item))
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"items": result})
}

func (h *Handler) deleteSupporterCode(w http.ResponseWriter, request *http.Request) {
	id, ok := pathID(w, request)
	if !ok {
		return
	}
	if err := h.service.DeleteSupporterCode(request.Context(), id); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]bool{"deleted": true})
}

func (h *Handler) redeemSupporter(w http.ResponseWriter, request *http.Request) {
	id, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		Code string `json:"code"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.RedeemSupporterCode(request.Context(), id, input.Code, strings.TrimSpace(request.Header.Get("Idempotency-Key")))
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, map[string]any{"code": item.Code, "rewardGold": item.RewardGold})
}

func (h *Handler) supporterDetails(w http.ResponseWriter, request *http.Request) {
	items, err := h.service.SupporterDetails(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, supporterDetailResponse(item))
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"items": result})
}

func (h *Handler) createSupporterDetail(w http.ResponseWriter, request *http.Request) {
	var input struct {
		Title   string `json:"title"`
		Details string `json:"details"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.CreateSupporterDetail(request.Context(), input.Title, input.Details)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, supporterDetailResponse(*item))
}

func (h *Handler) updateSupporterDetail(w http.ResponseWriter, request *http.Request) {
	id, ok := pathID(w, request)
	if !ok {
		return
	}
	var input struct {
		Title   string `json:"title"`
		Details string `json:"details"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.UpdateSupporterDetail(request.Context(), id, input.Title, input.Details)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, supporterDetailResponse(*item))
}

func (h *Handler) deleteSupporterDetail(w http.ResponseWriter, request *http.Request) {
	id, ok := pathID(w, request)
	if !ok {
		return
	}
	if err := h.service.DeleteSupporterDetail(request.Context(), id); err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]bool{"deleted": true})
}

func (h *Handler) publishNotice(w http.ResponseWriter, request *http.Request) {
	adminID, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		Title string `json:"title"`
		Body  string `json:"body"`
	}
	if !decode(w, request, &input) {
		return
	}
	item, err := h.service.PublishNotice(request.Context(), adminID, input.Title, input.Body)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, map[string]any{"id": item.ID, "title": item.Title, "body": item.Body, "publishedAt": item.PublishedAt})
}

func (h *Handler) notices(w http.ResponseWriter, request *http.Request) {
	items, err := h.service.Notices(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	result := make([]map[string]any, 0, len(items))
	for _, item := range items {
		result = append(result, map[string]any{"id": item.ID, "title": item.Title, "body": item.Body, "publishedAt": item.PublishedAt})
	}
	platformhttp.WriteData(w, http.StatusOK, map[string]any{"items": result})
}

func (h *Handler) publishContent(w http.ResponseWriter, request *http.Request) {
	adminID, ok := userID(w, request)
	if !ok {
		return
	}
	var input struct {
		Version string `json:"version"`
		Entries []struct {
			ChapterID string `json:"chapterId"`
			ID        string `json:"id"`
			MediaType string `json:"mediaType"`
			Body      string `json:"body"`
		} `json:"entries"`
	}
	if !decode(w, request, &input) {
		return
	}
	entries := make([]domain.ContentEntry, 0, len(input.Entries))
	for _, item := range input.Entries {
		entries = append(entries, domain.ContentEntry{ChapterID: item.ChapterID, ID: item.ID, MediaType: item.MediaType, Body: item.Body})
	}
	manifest, err := h.service.PublishContent(request.Context(), adminID, input.Version, entries)
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusCreated, manifestResponse(manifest))
}

func (h *Handler) contentManifest(w http.ResponseWriter, request *http.Request) {
	manifest, err := h.service.Content(request.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	platformhttp.WriteData(w, http.StatusOK, manifestResponse(manifest))
}

func (h *Handler) contentItem(w http.ResponseWriter, request *http.Request) {
	item, err := h.service.ContentItem(request.Context(), request.PathValue("id"))
	if err != nil {
		writeError(w, err)
		return
	}
	w.Header().Set("Content-Type", item.MediaType)
	w.Header().Set("ETag", `"`+item.SHA256+`"`)
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(item.Body))
}

func exchangeResponse(item *domain.Exchange) map[string]any {
	return map[string]any{"id": item.ID, "currencyCode": item.CurrencyCode, "quantity": item.Quantity, "rate": item.Rate, "goldGranted": item.GoldGranted, "balance": item.BalanceAfter, "gold": item.GoldAfter, "createdAt": item.CreatedAt}
}
func supporterDetailResponse(item domain.SupporterDetail) map[string]any {
	return map[string]any{"id": item.ID, "title": item.Title, "details": item.Details, "createdAt": item.CreatedAt, "updatedAt": item.UpdatedAt}
}
func supporterCodeResponse(item domain.SupporterCode) map[string]any {
	return map[string]any{"id": item.ID, "kind": item.Kind, "code": item.Code, "rewardGold": item.RewardGold, "status": item.Status, "redeemedBy": item.RedeemedBy, "redeemedAt": item.RedeemedAt, "createdAt": item.CreatedAt}
}
func manifestResponse(item *domain.ContentManifest) map[string]any {
	chapterOrder := make([]string, 0)
	chapterEntries := make(map[string][]map[string]any)
	for _, entry := range item.Entries {
		if _, exists := chapterEntries[entry.ChapterID]; !exists {
			chapterOrder = append(chapterOrder, entry.ChapterID)
		}
		chapterEntries[entry.ChapterID] = append(chapterEntries[entry.ChapterID], map[string]any{"id": entry.ID, "url": "items/" + entry.ID, "sha256": entry.SHA256, "type": entry.MediaType, "required": true})
	}
	chapters := make([]map[string]any, 0, len(chapterOrder))
	for _, chapterID := range chapterOrder {
		chapters = append(chapters, map[string]any{"chapterId": chapterID, "version": item.Version, "content": chapterEntries[chapterID]})
	}
	return map[string]any{"schemaVersion": "1", "chapters": chapters}
}
