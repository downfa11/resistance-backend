package server

import (
	"bufio"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"testing"
)

var routePattern = regexp.MustCompile(`router\.Handle\(http\.Method([A-Za-z]+), "([^"]+)"`)

func TestOpenAPICoversEveryRegisteredRoute(t *testing.T) {
	_, currentFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("resolve test source path")
	}
	root := filepath.Clean(filepath.Join(filepath.Dir(currentFile), "..", "..", ".."))
	sources := []string{
		"internal/platform/server/app.go",
		"internal/platform/auth/http.go",
		"internal/platform/notifications/http.go",
		"internal/resistance/httpapi/http.go",
	}
	routes := make(map[string]struct{})
	for _, source := range sources {
		payload, err := os.ReadFile(filepath.Join(root, filepath.FromSlash(source)))
		if err != nil {
			t.Fatalf("read %s: %v", source, err)
		}
		for _, match := range routePattern.FindAllStringSubmatch(string(payload), -1) {
			routes[strings.ToLower(match[1])+" "+match[2]] = struct{}{}
		}
	}

	specFile, err := os.Open(filepath.Join(root, "api", "openapi.yaml"))
	if err != nil {
		t.Fatalf("open OpenAPI contract: %v", err)
	}
	defer specFile.Close()
	documented := make(map[string]struct{})
	path := ""
	scanner := bufio.NewScanner(specFile)
	methodPattern := regexp.MustCompile(`^    (get|post|put|patch|delete):$`)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "  /") && strings.HasSuffix(line, ":") {
			path = strings.TrimSuffix(strings.TrimSpace(line), ":")
			continue
		}
		if path != "" {
			if match := methodPattern.FindStringSubmatch(line); match != nil {
				documented[match[1]+" "+path] = struct{}{}
			}
		}
	}
	if err := scanner.Err(); err != nil {
		t.Fatalf("scan OpenAPI contract: %v", err)
	}

	for route := range routes {
		if _, ok := documented[route]; !ok {
			t.Errorf("registered route missing from OpenAPI: %s", route)
		}
	}
	for route := range documented {
		if _, ok := routes[route]; !ok {
			t.Errorf("OpenAPI operation has no registered route: %s", route)
		}
	}
}

func TestOpenAPIDeclaresResistanceSecurityAndRawContent(t *testing.T) {
	_, currentFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("resolve test source path")
	}
	root := filepath.Clean(filepath.Join(filepath.Dir(currentFile), "..", "..", ".."))
	payload, err := os.ReadFile(filepath.Join(root, "api", "openapi.yaml"))
	if err != nil {
		t.Fatal(err)
	}
	spec := string(payload)
	for _, expected := range []string{
		"title: Resistance Server API",
		"security:\n  - bearerAuth: []",
		"application/octet-stream:\n              schema:\n                type: string\n                format: binary",
	} {
		if !strings.Contains(spec, expected) {
			t.Fatalf("OpenAPI contract missing %q", expected)
		}
	}
	if strings.Contains(spec, "Digimons") || strings.Contains(spec, "Arcade Server") {
		t.Fatal("OpenAPI contract still references a shared Arcade or Digimons server")
	}
}
