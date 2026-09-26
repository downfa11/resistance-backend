package httpapi

import (
	"encoding/json"
	"net/http"
)

type APIError struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func WriteData(w http.ResponseWriter, status int, data any) {
	writeJSON(w, status, struct {
		Data any `json:"data"`
	}{Data: data})
}

func WriteError(w http.ResponseWriter, status int, code, message string) {
	writeJSON(w, status, struct {
		Error APIError `json:"error"`
	}{Error: APIError{Code: code, Message: message}})
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
