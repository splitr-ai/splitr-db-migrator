package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class IdempotencyKeysMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("idempotency_keys table exists with all expected columns")
    void testTableAndColumnsExist() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'idempotency_keys' " +
                "ORDER BY ordinal_position"
        );

        assertThat(columns).hasSize(11);

        Map<String, String> columnTypes = new java.util.LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            columnTypes.put((String) col.get("column_name"), (String) col.get("data_type"));
        }

        assertThat(columnTypes).containsEntry("id", "character varying");
        assertThat(columnTypes).containsEntry("user_id", "character varying");
        assertThat(columnTypes).containsEntry("idempotency_key", "character varying");
        assertThat(columnTypes).containsEntry("request_fingerprint", "character varying");
        assertThat(columnTypes).containsEntry("request_path", "character varying");
        assertThat(columnTypes).containsEntry("response_status", "integer");
        assertThat(columnTypes).containsEntry("response_body", "text");
        assertThat(columnTypes).containsEntry("status", "character varying");
        assertThat(columnTypes).containsEntry("locked_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("expires_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("created_at", "timestamp with time zone");
    }

    @Test
    @DisplayName("status column defaults to PROCESSING")
    void testStatusDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'idempotency_keys' AND column_name = 'status'",
                String.class
        );
        assertThat(defaultValue).contains("PROCESSING");
    }

    @Test
    @DisplayName("created_at column defaults to NOW()")
    void testCreatedAtDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'idempotency_keys' AND column_name = 'created_at'",
                String.class
        );
        assertThat(defaultValue).isEqualToIgnoringCase("now()");
    }

    @Test
    @DisplayName("unique constraint on (user_id, idempotency_key) exists")
    void testUniqueConstraint() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'idempotency_keys' AND constraint_type = 'UNIQUE' " +
                "AND constraint_name = 'uq_idem_user_key'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("idx_idem_expires_at index exists")
    void testExpiresAtIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'idempotency_keys' AND indexname = 'idx_idem_expires_at'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("idx_idem_status_lock index exists")
    void testStatusLockIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'idempotency_keys' AND indexname = 'idx_idem_status_lock'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("primary key constraint exists on id column")
    void testPrimaryKey() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'idempotency_keys' AND constraint_type = 'PRIMARY KEY'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("NOT NULL constraints are correctly applied")
    void testNotNullConstraints() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'idempotency_keys' ORDER BY ordinal_position"
        );

        Map<String, String> nullability = new java.util.LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            nullability.put((String) col.get("column_name"), (String) col.get("is_nullable"));
        }

        // NOT NULL columns
        assertThat(nullability).containsEntry("id", "NO");
        assertThat(nullability).containsEntry("user_id", "NO");
        assertThat(nullability).containsEntry("idempotency_key", "NO");
        assertThat(nullability).containsEntry("status", "NO");
        assertThat(nullability).containsEntry("expires_at", "NO");
        assertThat(nullability).containsEntry("created_at", "NO");

        // Nullable columns
        assertThat(nullability).containsEntry("request_fingerprint", "YES");
        assertThat(nullability).containsEntry("request_path", "YES");
        assertThat(nullability).containsEntry("response_status", "YES");
        assertThat(nullability).containsEntry("response_body", "YES");
        assertThat(nullability).containsEntry("locked_at", "YES");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM idempotency_keys WHERE id LIKE 'test-%'");
    }

    @Test
    @DisplayName("CHECK constraint on status rejects invalid values")
    void testStatusCheckConstraint() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO idempotency_keys (id, user_id, idempotency_key, status, expires_at) " +
                        "VALUES ('test-chk', 'user-1', 'key-chk', 'INVALID', NOW() + INTERVAL '1 hour')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint allows valid status values")
    void testStatusCheckConstraintAllowsValidValues() {
        jdbcTemplate.update(
                "INSERT INTO idempotency_keys (id, user_id, idempotency_key, status, expires_at) " +
                "VALUES ('test-proc', 'user-1', 'key-proc', 'PROCESSING', NOW() + INTERVAL '1 hour')"
        );
        jdbcTemplate.update(
                "INSERT INTO idempotency_keys (id, user_id, idempotency_key, status, expires_at) " +
                "VALUES ('test-comp', 'user-1', 'key-comp', 'COMPLETED', NOW() + INTERVAL '1 hour')"
        );
        jdbcTemplate.update(
                "INSERT INTO idempotency_keys (id, user_id, idempotency_key, status, expires_at) " +
                "VALUES ('test-fail', 'user-1', 'key-fail', 'FAILED', NOW() + INTERVAL '1 hour')"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys WHERE id IN ('test-proc', 'test-comp', 'test-fail')",
                Integer.class
        );
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Duplicate (user_id, idempotency_key) is rejected by unique constraint")
    void testUniqueConstraintViolation() {
        jdbcTemplate.update(
                "INSERT INTO idempotency_keys (id, user_id, idempotency_key, expires_at) " +
                "VALUES ('test-dup-1', 'user-1', 'same-key', NOW() + INTERVAL '1 hour')"
        );

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO idempotency_keys (id, user_id, idempotency_key, expires_at) " +
                        "VALUES ('test-dup-2', 'user-1', 'same-key', NOW() + INTERVAL '1 hour')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
