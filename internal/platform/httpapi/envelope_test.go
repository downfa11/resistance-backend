package httpapi

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestWriteDataUsesCommonEnvelope(t *testing.T) {
	t.Parallel()

	recorder := httptest.NewRecorder()
	WriteData(recorder, http.StatusCreated, map[string]string{"id": "42"})

	if recorder.Code != http.StatusCreated {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusCreated)
	}
	if got := recorder.Header().Get("Content-Type"); got != "application/json; charset=utf-8" {
		t.Fatalf("Content-Type = %q", got)
	}
	var response struct {
		Data map[string]string `json:"data"`
	}
	if err := json.Unmarshal(recorder.Body.Bytes(), &response); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if response.Data["id"] != "42" {
		t.Fatalf("response = %#v", response)
	}
}

func TestWriteErrorUsesStableShape(t *testing.T) {
	t.Parallel()

	recorder := httptest.NewRecorder()
	WriteError(recorder, http.StatusConflict, "CONFLICT_RETRY", "Retry the request.")

	var response struct {
		Error APIError `json:"error"`
	}
	if err := json.Unmarshal(recorder.Body.Bytes(), &response); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if response.Error.Code != "CONFLICT_RETRY" || response.Error.Message != "Retry the request." {
		t.Fatalf("response = %#v", response)
	}
}
