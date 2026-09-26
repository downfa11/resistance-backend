package httpapi

import (
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestRequestIDCreatesAndPreservesIdentifier(t *testing.T) {
	t.Parallel()

	handler := RequestID(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if RequestIDFromContext(r.Context()) == "" {
			t.Fatal("request ID missing from context")
		}
		w.WriteHeader(http.StatusNoContent)
	}))

	generated := httptest.NewRecorder()
	handler.ServeHTTP(generated, httptest.NewRequest(http.MethodGet, "/", nil))
	if generated.Header().Get("X-Request-ID") == "" {
		t.Fatal("generated X-Request-ID is empty")
	}

	preserved := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodGet, "/", nil)
	request.Header.Set("X-Request-ID", "known-request-id")
	handler.ServeHTTP(preserved, request)
	if got := preserved.Header().Get("X-Request-ID"); got != "known-request-id" {
		t.Fatalf("X-Request-ID = %q", got)
	}
}

func TestRecoverReturnsSafeInternalError(t *testing.T) {
	t.Parallel()

	handler := Recover(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {
		panic("database credentials must not leak")
	}))
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodGet, "/", nil))

	if recorder.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d", recorder.Code)
	}
	if body := recorder.Body.String(); body == "" || strings.Contains(body, "credentials") {
		t.Fatalf("unsafe body = %q", body)
	}
}

func TestCORSAllowsOnlyConfiguredOrigin(t *testing.T) {
	t.Parallel()

	handler := CORS([]string{"https://resistance.example"}, http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	}))

	allowedRequest := httptest.NewRequest(http.MethodGet, "/", nil)
	allowedRequest.Header.Set("Origin", "https://resistance.example")
	allowed := httptest.NewRecorder()
	handler.ServeHTTP(allowed, allowedRequest)
	if got := allowed.Header().Get("Access-Control-Allow-Origin"); got != "https://resistance.example" {
		t.Fatalf("allowed origin header = %q", got)
	}

	deniedRequest := httptest.NewRequest(http.MethodGet, "/", nil)
	deniedRequest.Header.Set("Origin", "https://evil.example")
	denied := httptest.NewRecorder()
	handler.ServeHTTP(denied, deniedRequest)
	if got := denied.Header().Get("Access-Control-Allow-Origin"); got != "" {
		t.Fatalf("denied origin header = %q", got)
	}
}

func TestLimitBodyAppliesBoundedReader(t *testing.T) {
	t.Parallel()

	handler := LimitBody(4, http.HandlerFunc(func(w http.ResponseWriter, request *http.Request) {
		_, err := io.ReadAll(request.Body)
		var tooLarge *http.MaxBytesError
		if errors.As(err, &tooLarge) {
			WriteError(w, http.StatusRequestEntityTooLarge, "REQUEST_TOO_LARGE", "The request body is too large.")
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, httptest.NewRequest(http.MethodPost, "/", strings.NewReader("12345")))

	if recorder.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("status = %d, body = %s", recorder.Code, recorder.Body.String())
	}
}
