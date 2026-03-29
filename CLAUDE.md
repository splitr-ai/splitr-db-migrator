# Splitr DB Migrator

## What This Repo Is

This is a **Liquibase-based database migration project** for the Splitr expense-splitting app. The backend service lives in a separate repo — this repo contains **only database migrations**. Do not create application code, services, controllers, or any non-migration code here.

## Tech Stack

- **Java 21** with **Spring Boot 4.0.2**
- **Liquibase** for database migrations (via `spring-boot-starter-liquibase`)
- **PostgreSQL** as the target database
- **Gradle 9.3** with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- **AWS Advanced JDBC Wrapper** for RDS connectivity
- **Lombok** for annotations
- **Testcontainers** for integration testing (spins up isolated Postgres — no local DB dependency)

## Project Structure

```
src/main/resources/
├── db/schema/
│   ├── changelog.yml          # Liquibase master changelog (36 changesets as of V36)
│   ├── ddl/                   # Schema changes (CREATE, ALTER, etc.) — V1 through V36
│   └── dml/                   # Data changes (INSERT, UPDATE, etc.) — V2
├── cert/                      # AWS RDS certificates
├── application.yml            # Base config
├── application-local-postgres.yml
├── application-local-postgres-repave.yml
├── application-local-postgres-clean.yml
├── application-railway-dev.yml
└── application-railway-prod.yml
docs/adr/                      # Architecture Decision Records
src/test/java/                 # 24 integration test classes (including MigrationSmokeIntegrationTest)
```

## Database Tables (20 tables as of V36)

users, groups, group_members, expenses, expense_payers, expense_splits, settlements, categories, exchange_rates, activity_log, user_balances, guest_users, push_tokens, notification_log, shedlock, scan_usage, settlement_nudges, idempotency_keys, fx_snapshots, recurring_expense_templates

## Migration Conventions

- **DDL changes** go in `src/main/resources/db/schema/ddl/` with label `DDL`
- **DML changes** go in `src/main/resources/db/schema/dml/` with label `DML`
- File naming: `V{next_id}__{description}.sql` (double underscore for DDL, single for DML)
- Every new migration must be registered in `changelog.yml` with the next sequential changeset `id`
- **Next changeset ID: 37** (current highest is 36)
- Author field in changelog: `ajay`
- All tables use soft-delete (`is_deleted BOOLEAN`), optimistic locking (`version INTEGER`), and timestamp tracking (`created_at`, `updated_at`)
- Primary keys are `VARCHAR(60)` (UUID-based)
- Money amounts stored as `BIGINT` in smallest currency unit (cents)
- Currency codes validated with `~ '^[A-Z]{3}$'`
- Use partial indexes with `WHERE NOT is_deleted` where appropriate
- Add CHECK constraints for enum-like columns (status, type, frequency, etc.)

## Gradle Tasks

### Local
- `./gradlew migrate` — Run migrations on local Postgres
- `./gradlew updateDb` — Same as migrate (alias)
- `./gradlew repaveDb` — **DESTRUCTIVE.** Drop and recreate local database, then migrate
- `./gradlew cleanDb` — **DESTRUCTIVE.** Drop all objects and re-run all migrations

### Railway (Remote)
- `./gradlew migrateDev` — Run migrations on Railway dev (reads `.env.dev`)
- `./gradlew migrateProd` — Run migrations on Railway prod (reads `.env.prod`)

### Testing
- `./gradlew test` — Run all integration tests via Testcontainers (safe, no DB side effects)

## Pipeline

The **pipeline file** at `../pipeline/pipeline-db.md` (relative to this repo root) tracks pending DB migration tasks created by the BE team.

- **At the start of every session**, check `../pipeline/pipeline-db.md` for items with status `pending`.
- When you complete a pipeline item, update its status to `done` in `pipeline-db.md` and add the completion date in the Notes column.
- Spec files referenced in the pipeline are at `../pipeline/db/`.
- Related backend and frontend pipelines: `../pipeline/pipeline-be.md`, `../pipeline/pipeline-fe.md`

## Architecture Decision Records (ADRs)

ADRs live in `docs/adr/` within this repo. Use the format `ADR-{NNN}-{slug}.md`.

- Create an ADR when a pipeline item or migration involves a non-trivial architectural decision (new table design, index strategy, constraint philosophy, etc.).
- Reference the ADR from the migration SQL file header comment and from the pipeline notes.
- Current ADRs: ADR-001 (Idempotency layer design)

## Rules for Claude

- **Only create migration files** (SQL in `db/schema/ddl/` or `db/schema/dml/`) and update `changelog.yml`
- Do not modify Java source files unless explicitly asked
- Do not create new Java classes, Spring configurations, or application code
- Follow the existing changeset pattern in `changelog.yml` when adding new migrations
- Use the next sequential ID for new changesets
- **NEVER run `repaveDb` or `cleanDb`** — these are destructive and wipe the local database
- **NEVER run `migrate`, `updateDb`, `migrateDev`, or `migrateProd`** — Ajay applies migrations to local and Railway Postgres manually
- **Only run `./gradlew test`** to verify migrations — tests use Testcontainers (isolated Postgres), safe to run anytime
- After writing new migrations, always run `MigrationSmokeIntegrationTest` (and migration-specific tests) to validate
- When adding a new table, update the `expectedTables` set in `MigrationSmokeIntegrationTest`
- When adding a new changeset, bump the minimum ID assertion in `MigrationSmokeIntegrationTest.latestChangesetApplied()`

## Known Test Issue

`GuestUserEmailUniqueIndexIntegrationTest.testMultipleNullEmailsAreAllowed()` — pre-existing failure, unrelated to recent migrations.
