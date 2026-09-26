# Resistance Server

Go and SQLite backend for the Resistance client. This repository owns its own
users, sessions, notifications, game profile, social data, currency, content,
and administration API.

## Run locally

```powershell
Copy-Item .env.example .env
docker compose up --build
./scripts/smoke.ps1
```

The service listens on `http://127.0.0.1:8080`. SQLite and content assets live
under the persistent `/data` volume. Only one server replica may mount that
database.

## API

- OpenAPI: [`api/openapi.yaml`](api/openapi.yaml)
- Health: `GET /healthz`
- Readiness: `GET /readyz`
- Authentication: `/api/v1/auth/*`
- Account and inbox: `/api/v1/me`, `/api/v1/notifications/*`
- Resistance gameplay: `/api/v1/resistance/*`
- Resistance administration: `/api/v1/admin/resistance/*`

## Branch and deployment model

Feature pull requests target `deploy`. A successful merge to `deploy` builds
`ghcr.io/downfa11/resistance-server` and updates its immutable digest in
`downfa11/cluster-config`. Deployment secrets are managed in the cluster and
are never committed here.
