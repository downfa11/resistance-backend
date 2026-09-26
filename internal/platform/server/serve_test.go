package server

import (
	"net/http"
	"testing"
)

func TestHTTPServerHasConnectionDeadlines(t *testing.T) {
	t.Parallel()
	server := newHTTPServer(":0", http.NotFoundHandler())
	if server.ReadHeaderTimeout <= 0 || server.ReadTimeout <= 0 || server.WriteTimeout <= 0 || server.IdleTimeout <= 0 {
		t.Fatalf("server timeouts are not bounded: %#v", server)
	}
}
