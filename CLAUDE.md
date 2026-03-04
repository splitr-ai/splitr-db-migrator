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

## Project Structure

```
src/main/resources/
├── db/schema/
│   ├── changelog.yml          # Liquibase master changelog
│   ├── ddl/                   # Schema changes (CREATE, ALTER, etc.)
│   │   ├── V1__init_schema.sql
│   │   └── V3__add_created_at_group_member_table.sql
│   └── dml/                   # Data changes (INSERT, UPDATE, etc.)
│       └── V2_add_default_categories.sql
├── cert/                      # AWS RDS certificates
├── application.yml            # Base config
├── application-local-postgres.yml
└── application-local-postgres-repave.yml
```

## Migration Conventions

- **DDL changes** go in `src/main/resources/db/schema/ddl/` with label `DDL`
- **DML changes** go in `src/main/resources/db/schema/dml/` with label `DML`
- File naming: `V{next_id}__{description}.sql` (double underscore for DDL, single for DML)
- Every new migration must be registered in `changelog.yml` with the next sequential changeset `id`
- Author field in changelog: `ajay`
- All tables use soft-delete (`is_deleted BOOLEAN`), optimistic locking (`version INTEGER`), and timestamp tracking (`created_at`, `updated_at`)
- Primary keys are `VARCHAR(60)` (UUID-based)
- Money amounts stored as `BIGINT` in smallest currency unit (cents)
- Currency codes validated with `~ '^[A-Z]{3}$'`
- Use partial indexes with `WHERE NOT is_deleted` where appropriate

## Gradle Tasks

- `./gradlew migrate` — Run migrations on local Postgres
- `./gradlew updateDb` — Same as migrate (alias)
- `./gradlew repaveDb` — Drop and recreate the database, then run migrate

## Rules for Claude

- **Only create migration files** (SQL in `db/schema/ddl/` or `db/schema/dml/`) and update `changelog.yml`
- Do not modify Java source files unless explicitly asked
- Do not create new Java classes, Spring configurations, or application code
- Follow the existing changeset pattern in `changelog.yml` when adding new migrations
- Use the next sequential ID for new changesets
