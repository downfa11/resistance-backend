# Resistance Source Inventory

The port source is the committed `wargame-server` Resistance implementation
plus the active Unity request and content-delivery code. The legacy Java and
Python servers explain old behavior only; their URL shapes are not preserved.

## Behavior to preserve

- Idempotent profile creation with server-owned identity and economy fields.
- Explicit profile patch semantics: omitted fields preserve stored values.
- Directional friend requests, canonical undirected friendships, duplicate and
  self-request rejection, and participant-only reads/deletes.
- Usage-weighted currency exchange with module-owned balances and Gold.
- Exact-once exchanges and supporter-code redemption through idempotency keys.
- Administrator exchange-rate, supporter, notice, and content management.
- Versioned content manifests with SHA-256 verification metadata.
- Server-local inbox notifications for friend requests, acceptance, published
  notices, exchanges, and supporter redemption.

## New API families

- `/api/v1/resistance/me/*`
- `/api/v1/resistance/friends/*`
- `/api/v1/resistance/exchanges/*`
- `/api/v1/resistance/supporters/*`
- `/api/v1/resistance/content/*`
- `/api/v1/admin/resistance/*`

Resistance Gold, currencies, identities, sessions, and notifications belong to
this server. Legacy `/membership/*`, dedicated, and business service routes are
intentionally removed.
