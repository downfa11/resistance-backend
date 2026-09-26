package config

import (
	"errors"
	"fmt"
	"strings"
)

const minimumProductionSecretBytes = 32

var ErrInvalid = errors.New("invalid Resistance server configuration")

type Config struct {
	Environment    string
	Addr           string
	DBPath         string
	JWTSecret      string
	AllowedOrigins []string
	AssetRoot      string
}

type LookupFunc func(string) string

func Load(lookup LookupFunc) (Config, error) {
	if lookup == nil {
		return Config{}, fmt.Errorf("%w: environment lookup is required", ErrInvalid)
	}

	cfg := Config{
		Environment: strings.ToLower(valueOrDefault(lookup("RESISTANCE_ENV"), "development")),
		Addr:        valueOrDefault(lookup("RESISTANCE_ADDR"), ":8080"),
		DBPath:      valueOrDefault(lookup("RESISTANCE_DB_PATH"), "data/resistance.db"),
		JWTSecret:   strings.TrimSpace(lookup("RESISTANCE_JWT_SECRET")),
		AssetRoot:   valueOrDefault(lookup("RESISTANCE_ASSET_ROOT"), "data/assets"),
	}
	cfg.AllowedOrigins = splitUnique(lookup("RESISTANCE_ALLOWED_ORIGINS"))

	switch cfg.Environment {
	case "development", "test", "production":
	default:
		return Config{}, fmt.Errorf("%w: unsupported RESISTANCE_ENV %q", ErrInvalid, cfg.Environment)
	}
	if cfg.JWTSecret == "" {
		return Config{}, fmt.Errorf("%w: RESISTANCE_JWT_SECRET is required", ErrInvalid)
	}
	if cfg.Environment == "production" && len([]byte(cfg.JWTSecret)) < minimumProductionSecretBytes {
		return Config{}, fmt.Errorf("%w: production RESISTANCE_JWT_SECRET must be at least %d bytes", ErrInvalid, minimumProductionSecretBytes)
	}

	return cfg, nil
}

func valueOrDefault(value, fallback string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return fallback
	}
	return value
}

func splitUnique(value string) []string {
	if strings.TrimSpace(value) == "" {
		return nil
	}

	seen := make(map[string]struct{})
	result := make([]string, 0)
	for _, item := range strings.Split(value, ",") {
		item = strings.TrimSpace(item)
		if item == "" {
			continue
		}
		if _, ok := seen[item]; ok {
			continue
		}
		seen[item] = struct{}{}
		result = append(result, item)
	}
	return result
}
