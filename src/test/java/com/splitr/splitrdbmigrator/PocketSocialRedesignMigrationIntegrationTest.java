package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DB-16, DB-17, DB-18 (V44 to V52): notification_log.read_at with its unread index,
 * and expenses/settlements.updated_by with a validated FK and a partial index.
 */
@SpringBootTest
@ActiveProfiles("local-postgres")
class PocketSocialRedesignMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @ParameterizedTest(name = "{0}.{1} is {2}, nullable")
    @CsvSource({
            "notification_log, read_at, timestamp with time zone",
            "expenses, updated_by, character varying",
            "settlements, updated_by, character varying"
    })
    @DisplayName("new columns exist with the expected type and are nullable")
    void columnsExist(final String table, final String column, final String type) {
        final Map<String, Object> meta = jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable, character_maximum_length FROM information_schema.columns "
                        + "WHERE table_name = ? AND column_name = ?", table, column);

        assertThat(meta.get("data_type")).isEqualTo(type);
        assertThat(meta.get("is_nullable")).isEqualTo("YES");
        if ("character varying".equals(type)) {
            assertThat(((Number) meta.get("character_maximum_length")).intValue()).isEqualTo(60);
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "idx_notification_log_user_unread, notification_log, (read_at IS NULL)",
            "idx_expenses_updated_by, expenses, (updated_by IS NOT NULL)",
            "idx_settlements_updated_by, settlements, (updated_by IS NOT NULL)"
    })
    @DisplayName("partial indexes exist, are valid, and carry the expected predicate")
    void partialIndexesExist(final String index, final String table, final String predicate) {
        final Map<String, Object> meta = jdbcTemplate.queryForMap(
                "SELECT i.indisvalid AS valid, pg_get_expr(i.indpred, i.indrelid) AS predicate, t.relname AS tbl "
                        + "FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
                        + "JOIN pg_class t ON t.oid = i.indrelid WHERE c.relname = ?", index);

        assertThat(meta.get("valid")).isEqualTo(true);
        assertThat(meta.get("tbl")).isEqualTo(table);
        assertThat(meta.get("predicate")).isEqualTo(predicate);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"fk_expenses_updated_by, expenses", "fk_settlements_updated_by, settlements"})
    @DisplayName("updated_by FKs reference users and are validated")
    void foreignKeysAreValidated(final String constraint, final String table) {
        final Map<String, Object> meta = jdbcTemplate.queryForMap(
                "SELECT con.convalidated AS validated, con.confrelid::regclass::text AS target, "
                        + "con.conrelid::regclass::text AS source "
                        + "FROM pg_constraint con WHERE con.conname = ? AND con.contype = 'f'", constraint);

        assertThat(meta.get("validated")).isEqualTo(true);
        assertThat(meta.get("target")).isEqualTo("users");
        assertThat(meta.get("source")).isEqualTo(table);
    }

    @Test
    @DisplayName("V45 backfill marks an unread notification as read at its creation time")
    void backfillMarksExistingNotificationsRead() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, clerk_id, email, name) VALUES ('test-psr-nu', 'clerk-test-psr-nu', "
                + "'test-psr-nu@test.com', 'T')");
        jdbcTemplate.update("INSERT INTO notification_log (id, user_id, notification_type, title, body, created_at) "
                + "VALUES ('test-psr-n1', 'test-psr-nu', 'test_type', 't', 'b', TIMESTAMPTZ '2026-01-02 03:04:05+00')");

        runSql("db/schema/dml/V45_backfill_notification_log_read_at.sql");

        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT read_at = created_at AS same FROM notification_log WHERE id = 'test-psr-n1'");
        assertThat(row.get("same")).isEqualTo(true);
    }

    private void runSql(final String classpathFile) throws Exception {
        jdbcTemplate.execute(new String(new ClassPathResource(classpathFile).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM notification_log WHERE id LIKE 'test-psr-%'");
        jdbcTemplate.update("DELETE FROM group_members WHERE id LIKE 'test-psr-%'");
        jdbcTemplate.update("DELETE FROM groups WHERE id LIKE 'test-psr-%'");
        jdbcTemplate.update("DELETE FROM guest_users WHERE id LIKE 'test-psr-%'");
        jdbcTemplate.update("DELETE FROM users WHERE id LIKE 'test-psr-%'");
    }

    @Test
    @DisplayName("V53 reactivates deleted rows bound to a user and leaves guest-only deleted rows alone")
    void reactivationPatchFixesOnlyLockedOutUsers() throws Exception {
        for (final String user : List.of("test-psr-u1", "test-psr-u2")) {
            jdbcTemplate.update("INSERT INTO users (id, clerk_id, email, name) VALUES (?, ?, ?, 'T')",
                    user, "clerk-" + user, user + "@test.com");
        }
        jdbcTemplate.update("INSERT INTO groups (id, name, default_currency, created_by) "
                + "VALUES ('test-psr-g1', 'G', 'USD', 'test-psr-u1')");
        jdbcTemplate.update("INSERT INTO guest_users (id, name) VALUES ('test-psr-gst1', 'Guest'), ('test-psr-gst2', 'Guest')");
        // u1: locked out (only a deleted row bound to the user) -> reactivated
        member("test-psr-m1", "test-psr-u1", "test-psr-gst1", true);
        // u2: active row -> untouched
        member("test-psr-m2", "test-psr-u2", null, false);
        // guest-only deleted row (no user) -> left deleted
        member("test-psr-m4", null, "test-psr-gst2", true);

        runSql("db/schema/dml/V53_reactivate_user_bound_deleted_memberships.sql");

        assertThat(deleted("test-psr-m1")).isFalse();
        assertThat(deleted("test-psr-m2")).isFalse();
        assertThat(deleted("test-psr-m4")).isTrue();
    }

    private void member(final String id, final String userId, final String guestId, final boolean isDeleted) {
        jdbcTemplate.update("INSERT INTO group_members (id, group_id, user_id, guest_user_id, is_deleted) "
                + "VALUES (?, 'test-psr-g1', ?, ?, ?)", id, userId, guestId, isDeleted);
    }

    private boolean deleted(final String id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM group_members WHERE id = ?", Boolean.class, id));
    }
}
