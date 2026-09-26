package httpapi

import (
	"bytes"
	"net/http"
	"strings"
)

type Router struct {
	mux *http.ServeMux
}

func NewRouter() *Router {
	return &Router{mux: http.NewServeMux()}
}

func (r *Router) Handle(method, path string, handler http.Handler) {
	method = strings.ToUpper(strings.TrimSpace(method))
	if method == "" || path == "" || handler == nil {
		panic("httpapi: method, path, and handler are required")
	}
	r.mux.Handle(method+" "+path, handler)
}

func (r *Router) ServeHTTP(w http.ResponseWriter, request *http.Request) {
	handler, pattern := r.mux.Handler(request)
	if pattern != "" {
		r.mux.ServeHTTP(w, request)
		return
	}

	unmatched := &unmatchedResponse{header: make(http.Header)}
	handler.ServeHTTP(unmatched, request)
	if allow := unmatched.header.Get("Allow"); allow != "" {
		w.Header().Set("Allow", allow)
	}
	if unmatched.status == http.StatusMethodNotAllowed {
		WriteError(w, http.StatusMethodNotAllowed, "METHOD_NOT_ALLOWED", "The request method is not allowed for this route.")
		return
	}
	WriteError(w, http.StatusNotFound, "ROUTE_NOT_FOUND", "The requested route does not exist.")
}

type unmatchedResponse struct {
	header http.Header
	status int
	body   bytes.Buffer
}

func (r *unmatchedResponse) Header() http.Header { return r.header }

func (r *unmatchedResponse) WriteHeader(status int) {
	if r.status == 0 {
		r.status = status
	}
}

func (r *unmatchedResponse) Write(body []byte) (int, error) {
	if r.status == 0 {
		r.status = http.StatusOK
	}
	return r.body.Write(body)
}
