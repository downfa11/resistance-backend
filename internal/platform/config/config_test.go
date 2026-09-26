package config

import (
	"reflect"
	"testing"
)

func TestLoadUsesDevelopmentDefaults(t *testing.T) {
	t.Parallel()

	values := map[string]string{
		"RESISTANCE_JWT_SECRET": "development-secret",
	}

	got, err := Load(func(key string) string { return values[key] })
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	if got.Environment != "development" {
		t.Fatalf("Environment = %q, want development", got.Environment)
	}
	if got.Addr != ":8080" {
		t.Fatalf("Addr = %q, want :8080", got.Addr)
	}
	if got.DBPath != "data/resistance.db" {
		t.Fatalf("DBPath = %q, want data/resistance.db", got.DBPath)
	}
	if got.AssetRoot != "data/assets" {
		t.Fatalf("AssetRoot = %q, want data/assets", got.AssetRoot)
	}
	if len(got.AllowedOrigins) != 0 {
		t.Fatalf("AllowedOrigins = %#v, want empty", got.AllowedOrigins)
	}
}

func TestLoadNormalizesConfiguredValues(t *testing.T) {
	t.Parallel()

	values := map[string]string{
		"RESISTANCE_ENV":                      " production ",
		"RESISTANCE_ADDR":                     " 127.0.0.1:9090 ",
		"RESISTANCE_DB_PATH":                  " var/resistance.sqlite ",
		"RESISTANCE_JWT_SECRET":               "0123456789abcdef0123456789abcdef",
		"RESISTANCE_ALLOWED_ORIGINS":          " https://resistance.example, https://resistance.example ",
		"RESISTANCE_ASSET_ROOT":               " var/assets ",
		"RESISTANCE_BOOTSTRAP_ADMIN_ACCOUNT":  " root.admin ",
		"RESISTANCE_BOOTSTRAP_ADMIN_EMAIL":    " ROOT@Example.com ",
		"RESISTANCE_BOOTSTRAP_ADMIN_PASSWORD": "admin-password",
	}

	got, err := Load(func(key string) string { return values[key] })
	if err != nil {
		t.Fatalf("Load() error = %v", err)
	}

	if got.Environment != "production" || got.Addr != "127.0.0.1:9090" || got.DBPath != "var/resistance.sqlite" || got.AssetRoot != "var/assets" {
		t.Fatalf("Load() = %#v", got)
	}
	wantOrigins := []string{"https://resistance.example"}
	if !reflect.DeepEqual(got.AllowedOrigins, wantOrigins) {
		t.Fatalf("AllowedOrigins = %#v, want %#v", got.AllowedOrigins, wantOrigins)
	}
	if got.BootstrapAdminAccount != "root.admin" || got.BootstrapAdminEmail != "root@example.com" || got.BootstrapAdminPassword != "admin-password" {
		t.Fatalf("bootstrap administrator = %#v", got)
	}
}

func TestLoadRejectsPartialBootstrapAdministrator(t *testing.T) {
	t.Parallel()
	values := map[string]string{
		"RESISTANCE_JWT_SECRET":              "development-secret",
		"RESISTANCE_BOOTSTRAP_ADMIN_ACCOUNT": "admin",
	}
	if _, err := Load(func(key string) string { return values[key] }); err == nil {
		t.Fatal("Load() error = nil, want partial administrator error")
	}
}

func TestLoadRejectsMissingSecret(t *testing.T) {
	t.Parallel()

	if _, err := Load(func(string) string { return "" }); err == nil {
		t.Fatal("Load() error = nil, want missing secret error")
	}
}

func TestLoadRejectsWeakProductionSecret(t *testing.T) {
	t.Parallel()

	values := map[string]string{
		"RESISTANCE_ENV":        "production",
		"RESISTANCE_JWT_SECRET": "too-short",
	}

	if _, err := Load(func(key string) string { return values[key] }); err == nil {
		t.Fatal("Load() error = nil, want weak production secret error")
	}
}

func TestLoadRejectsUnsupportedEnvironment(t *testing.T) {
	t.Parallel()

	values := map[string]string{
		"RESISTANCE_ENV":        "staging-ish",
		"RESISTANCE_JWT_SECRET": "development-secret",
	}

	if _, err := Load(func(key string) string { return values[key] }); err == nil {
		t.Fatal("Load() error = nil, want environment error")
	}
}
