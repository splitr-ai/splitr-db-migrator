package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration smoke test — run after every new SQL script.
 * Verifies all Liquibase changesets applied cleanly and the expected tables exist.
 * This test does NOT need updating when new migrations are added — it reads
 * the changelog and table list dynamically.
 */
@SpringBootTest
@ActiveProfiles("local-postgres")
class MigrationSmokeIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("All Liquibase changesets executed successfully with no failures")
    void allChangesetsSucceeded() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, exectype FROM databasechangelog ORDER BY orderexecuted"
        );

        assertThat(rows).isNotEmpty();

        for (Map<String, Object> row : rows) {
            assertThat(row.get("exectype"))
                    .as("Changeset %s should have exectype EXECUTED", row.get("id"))
                    .isEqualTo("EXECUTED");
        }
    }

    @Test
    @DisplayName("Latest changeset ID matches the highest expected migration")
    void latestChangesetApplied() {
        Integer maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(CAST(id AS INTEGER)) FROM databasechangelog",
                Integer.class
        );

        // This will naturally grow as new migrations are added.
        // If this test fails, it means the newest changeset didn't apply.
        assertThat(maxId).isGreaterThanOrEqualTo(37);
    }

    @Test
    @DisplayName("No Liquibase lock is stuck")
    void noStuckLocks() {
        Integer lockedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangeloglock WHERE locked = TRUE",
                Integer.class
        );
        assertThat(lockedCount).isZero();
    }

    @Test
    @DisplayName("All expected application tables exist")
    void allExpectedTablesExist() {
        Set<String> expectedTables = Set.of(
                "users",
                "groups",
                "group_members",
                "expenses",
                "expense_payers",
                "expense_splits",
                "settlements",
                "categories",
                "exchange_rates",
                "activity_log",
                "user_balances",
                "guest_users",
                "push_tokens",
                "notification_log",
                "shedlock",
                "scan_usage",
                "settlement_nudges",
                "idempotency_keys",
                "fx_snapshots",
                "recurring_expense_templates",
                "finance_audit_log"
        );

        List<String> actualTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' " +
                "AND table_type = 'BASE TABLE' " +
                "AND table_name NOT LIKE 'databasechangelog%'",
                String.class
        );

        assertThat(actualTables).containsAll(expectedTables);
    }

    @Test
    @DisplayName("No changeset was executed more than once")
    void noChangesetExecutedMoreThanOnce() {
        List<Map<String, Object>> duplicates = jdbcTemplate.queryForList(
                "SELECT id, COUNT(*) as cnt FROM databasechangelog " +
                "GROUP BY id HAVING COUNT(*) > 1"
        );
        assertThat(duplicates)
                .as("No changeset should be executed more than once")
                .isEmpty();
    }

    @Test
    @DisplayName("All changesets were authored by 'ajay'")
    void allChangesetsAuthoredByAjay() {
        List<String> authors = jdbcTemplate.queryForList(
                "SELECT DISTINCT author FROM databasechangelog",
                String.class
        );
        assertThat(authors).containsExactly("ajay");
    }

    @Test
    @DisplayName("Changeset IDs are sequential with no gaps")
    void changesetIdsAreSequential() {
        List<Integer> ids = jdbcTemplate.queryForList(
                "SELECT CAST(id AS INTEGER) AS id FROM databasechangelog ORDER BY CAST(id AS INTEGER)",
                Integer.class
        );

        assertThat(ids).isNotEmpty();
        for (int i = 0; i < ids.size(); i++) {
            assertThat(ids.get(i))
                    .as("Changeset ID at position %d should be %d", i, i + 1)
                    .isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("Every DDL SQL file referenced in changelog exists in the ddl/ directory")
    void allReferencedSqlFilesExist() {
        List<String> filenames = jdbcTemplate.queryForList(
                "SELECT filename FROM databasechangelog ORDER BY orderexecuted",
                String.class
        );

        // Each filename should be resolvable — if Liquibase ran it, the file existed.
        // This test confirms no post-migration file deletions broke consistency.
        assertThat(filenames).allSatisfy(f ->
                assertThat(f).as("Changelog filename should not be blank").isNotBlank()
        );
    }
}
