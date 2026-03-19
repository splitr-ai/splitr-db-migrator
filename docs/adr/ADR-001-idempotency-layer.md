# ADR-001: Idempotency Layer via `idempotency_keys` Table

**Status:** Accepted
**Date:** 2026-03-18
**Author:** Ajay Wadhara

## Context

Splitr's mobile clients retry failed POST requests (expense creation, settlements, group joins). Without server-side idempotency, retries can create duplicate expenses or double-settle debts. A database-backed idempotency layer guarantees exactly-once semantics for mutating API calls.

## Decision

Introduce an `idempotency_keys` table that stores a composite unique key of `(user_id, idempotency_key)`. The backend will:

1. On every mutating request, check for an existing key before processing.
2. If found and completed, return the cached response (`response_status`, `response_body`).
3. If found and still `PROCESSING`, return `409 Conflict` (concurrent duplicate).
4. If not found, insert a row with status `PROCESSING`, execute the operation, then update the row with the response.

### Key Design Choices

- **TTL-based expiry** — `expires_at` column, rows pruned by a scheduled reaper job (ShedLock). Default TTL: 24 hours.
- **Optimistic locking via `status` + `locked_at`** — avoids row-level locks during normal flow; the `idx_idem_status_lock` index supports the reaper query.
- **`request_fingerprint` (SHA-256 of body)** — optional guard to detect mismatched bodies reusing the same idempotency key.
- **Status lifecycle: `PROCESSING → COMPLETED | FAILED`** — enforced by a CHECK constraint. `FAILED` captures operations that error out, allowing the client to retry with the same key. The reaper cleans up all expired rows regardless of terminal status.
- **No soft-delete** — expired rows are hard-deleted by the reaper. Idempotency keys are ephemeral by nature; soft-delete adds no value here.
- **No `version` / `updated_at`** — rows transition `PROCESSING → COMPLETED/FAILED` once and are never updated again.
- **No FK on `user_id`** — intentional. This table is ephemeral with hard-delete semantics. A foreign key would complicate the reaper and add unnecessary lock contention for no referential integrity benefit on a 24-hour TTL table.

### `response_body` Sensitivity

The `response_body` column caches the full HTTP response body for replay on duplicate requests. This may contain PII (user names, expense amounts, group details). Mitigations:

- **Encryption at rest** — RDS encryption covers this column at the storage layer.
- **24-hour TTL** — data is short-lived; the reaper hard-deletes expired rows.
- **Backend responsibility** — the application layer should avoid caching responses that contain auth tokens or credentials. Standard API responses (expense created, settlement recorded) are acceptable.
- **No direct user access** — this table is internal; no API endpoint exposes cached responses to users other than the original requester via idempotency replay.

## Consequences

- Mutating endpoints gain exactly-once semantics with no client-side changes beyond sending an `Idempotency-Key` header.
- The reaper job depends on the existing `shedlock` table (V11).
- Adds one write per mutating request; mitigated by the 24-hour TTL keeping the table small.
- The `idempotency_keys` table does not follow the project-wide soft-delete / optimistic-locking conventions — this is intentional and documented above.

## References

- Pipeline: DB-1
- Migration: `V29__create_idempotency_keys_table.sql`
