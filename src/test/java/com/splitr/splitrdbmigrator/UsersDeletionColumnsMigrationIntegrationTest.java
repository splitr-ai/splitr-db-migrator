package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class UsersDeletionColumnsMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private void insertUser(String isDeleted, String deletedAt, String clerkDeletedAt) {
        String suffix = Long.toString(System.nanoTime());
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name, referral_code, is_deleted, deleted_at, clerk_deleted_at) "
                        + "VALUES (?, ?, ?, 'Del', ?, " + isDeleted + ", " + deletedAt + ", " + clerkDeletedAt + ")",
                "usr_del_" + suffix, "clerk_del_" + suffix, "del-" + suffix + "@example.com", "D" + suffix);
    }

    @ParameterizedTest
    @ValueSource(strings = {"deleted_at", "clerk_deleted_at"})
    @DisplayName("deletion column is a nullable timestamptz")
    void deletionColumnShape(String name) {
        Map<String, Object> col = jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'users' AND column_name = ?", name);
        assertThat(col.get("data_type")).isEqualTo("timestamp with time zone");
        assertThat(col.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    @DisplayName("is_deleted true with no deleted_at fails on chk_users_deleted_state")
    void deletedWithoutDeletedAtRejected() {
        assertThatThrownBy(() -> insertUser("TRUE", "NULL", "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_deleted_state");
    }

    @Test
    @DisplayName("is_deleted false with deleted_at set fails on chk_users_deleted_state")
    void activeWithDeletedAtRejected() {
        assertThatThrownBy(() -> insertUser("FALSE", "NOW()", "NULL"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_deleted_state");
    }

    @Test
    @DisplayName("clerk_deleted_at set with no deleted_at fails on chk_users_clerk_deleted")
    void clerkDeletedWithoutDeletedAtRejected() {
        assertThatThrownBy(() -> insertUser("FALSE", "NULL", "NOW()"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_clerk_deleted");
    }

    @Test
    @DisplayName("a deleted user with both timestamps is accepted")
    void fullyDeletedUserAccepted() {
        String suffix = Long.toString(System.nanoTime());
        String id = "usr_ok_" + suffix;
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name, referral_code, is_deleted, deleted_at, clerk_deleted_at) "
                        + "VALUES (?, ?, ?, 'Del', ?, TRUE, NOW(), NOW())",
                id, "clerk_ok_" + suffix, "ok-" + suffix + "@example.com", "O" + suffix);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    @DisplayName("both CHECK constraints are validated")
    void checksValidated() {
        Integer validated = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conrelid = 'users'::regclass "
                        + "AND conname IN ('chk_users_deleted_state', 'chk_users_clerk_deleted') AND convalidated",
                Integer.class);
        assertThat(validated).isEqualTo(2);
    }

    @Test
    @DisplayName("idx_users_deletion_cleanup exists, is valid and is partial")
    void cleanupIndexIsValidAndPartial() {
        Map<String, Object> idx = jdbcTemplate.queryForMap(
                "SELECT i.indisvalid AS valid, pg_get_expr(i.indpred, i.indrelid) AS predicate, t.relname AS tbl "
                        + "FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
                        + "JOIN pg_class t ON t.oid = i.indrelid WHERE c.relname = ?",
                "idx_users_deletion_cleanup");
        assertThat(idx.get("tbl")).isEqualTo("users");
        assertThat(idx.get("valid")).isEqualTo(true);
        assertThat((String) idx.get("predicate"))
                .contains("deleted_at IS NOT NULL")
                .contains("clerk_deleted_at IS NULL")
                .contains("profile_image_key IS NOT NULL");
    }
}
