package httpapi

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestRouterNormalizesMissingRouteAndMethod(t *testing.T) {
	t.Parallel()

	router := NewRouter()
	router.Handle(http.MethodGet, "/resource", http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		WriteData(w, http.StatusOK, map[string]bool{"ok": true})
	}))

	tests := []struct {
		name       string
		method     string
		path       string
		wantStatus int
		wantCode   string
	}{
		{name: "missing", method: http.MethodGet, path: "/missing", wantStatus: http.StatusNotFound, wantCode: "ROUTE_NOT_FOUND"},
		{name: "method", method: http.MethodPost, path: "/resource", wantStatus: http.StatusMethodNotAllowed, wantCode: "METHOD_NOT_ALLOWED"},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			recorder := httptest.NewRecorder()
			router.ServeHTTP(recorder, httptest.NewRequest(tc.method, tc.path, nil))
			if recorder.Code != tc.wantStatus {
				t.Fatalf("status = %d, want %d", recorder.Code, tc.wantStatus)
			}
			var response struct {
				Error APIError `json:"error"`
			}
			if err := json.Unmarshal(recorder.Body.Bytes(), &response); err != nil {
				t.Fatalf("decode response: %v", err)
			}
			if response.Error.Code != tc.wantCode {
				t.Fatalf("code = %q, want %q", response.Error.Code, tc.wantCode)
			}
		})
	}
}
