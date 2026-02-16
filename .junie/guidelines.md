### Project Guidelines: splitr-db-migrator

#### 1. Build & Configuration

This project is a Spring Boot-based database migrator using Liquibase. It is designed to manage schema (DDL) and data (DML) migrations for the Splitr application.

##### Environment Prerequisites
- **Java 21**
- **PostgreSQL** (running locally for `local-postgres` profiles)

##### Gradle Tasks
Specific Gradle tasks are registered in `build.gradle.kts` for common operations:
- `./gradlew migrate`: Runs migrations (DDL & DML) against the local Postgres database (`splitr-db`).
- `./gradlew updateDb`: Alias for `migrate`, runs Liquibase updates without repaving.
- `./gradlew repaveDb`: **Destructive operation**. Drops the existing `splitr-db` database and recreates it from scratch before running migrations.

##### Spring Profiles
- `local-postgres`: Standard local development profile.
- `local-postgres-repave`: Used specifically for the repave operation.

#### 2. Liquibase Migrations

Migrations are managed via Liquibase. The master changelog is located at `src/main/resources/db/schema/changelog.yml`.

##### Adding New Migrations
1. Create a new `.sql` file in the appropriate directory:
   - `src/main/resources/db/schema/ddl/` for schema changes.
   - `src/main/resources/db/schema/dml/` for data changes.
2. Follow the naming convention: `V<Number>__<Description>.sql` (e.g., `V27__add_new_table.sql`).
3. Register the new file in `changelog.yml` by adding a new `changeSet`. Assign a unique `id` and appropriate `labels` (`DDL` or `DML`).

Example `changeSet`:
```yaml
  - changeSet:
      id: 27
      author: developer
      labels: DDL
      changes:
        - sqlFile:
            path: ddl/V27__add_new_table.sql
            relativeToChangelogFile: true
```

#### 3. Testing Information

##### Running Tests
- Use `./gradlew test` to run all tests.
- Note: Integration tests that load the Spring context (like `SplitrDbMigratorApplicationTests`) require a connection to a PostgreSQL database as configured in the active profile.

##### Adding New Tests
When adding new migrations, it is recommended to add an integration test to verify the migration applied correctly.

**Example Integration Test:**
```java
package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class MigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testNewTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'your_new_table'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
```

#### 4. Development Standards

- **Lombok**: The project uses Lombok for reducing boilerplate (e.g., `@Slf4j`, `@RequiredArgsConstructor`).
- **DB Repave Logic**: The repave logic is implemented in `LocalPostgresDbRepave.java` and is triggered by the `local-postgres-repave` profile and the `postgres.repave=true` property.
- **Naming Conventions**: Keep SQL migration files sequentially numbered to avoid conflicts and maintain a clear history.
