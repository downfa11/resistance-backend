# Resistance Server Operations

Run locally with `go run ./cmd/server` after setting `RESISTANCE_JWT_SECRET`, or copy
`.env.example` to `.env` and run `docker compose up --build`.

Set `RESISTANCE_BOOTSTRAP_ADMIN_ACCOUNT`, `RESISTANCE_BOOTSTRAP_ADMIN_EMAIL`, and
`RESISTANCE_BOOTSTRAP_ADMIN_PASSWORD` together to create the first administrator.
The bootstrap is idempotent: restarting with the same account keeps its user id,
restores the `ADMIN`/`ACTIVE` state, and rotates the password to the configured
value. Keep these values in the deployment secret, never in Git.

SQLite uses WAL, foreign keys, a five-second busy timeout, and one process-local
writer connection. Mount `/data` on persistent storage. Run only one server
instance against a database file; horizontal replicas require a different
database architecture.

## Backup and restore

Prefer SQLite's online backup API or `VACUUM INTO` from a coordinated
administrative process. The simple alternative is to stop Resistance Server,
checkpoint WAL, and copy `resistance.db` together with the asset directory. Never
copy only the live main database file while WAL writes are active.

Restore into an empty data directory, keep the original backup unchanged, and
start the same or newer server version. Embedded Goose migrations run at every
startup and are idempotent.

## Health and smoke

- `GET /healthz` reports process health.
- `GET /readyz` pings SQLite.
- `scripts/smoke.ps1` creates an account, initializes and updates its Resistance
  profile, reads balances and the inbox, rotates the refresh token, and logs out.
