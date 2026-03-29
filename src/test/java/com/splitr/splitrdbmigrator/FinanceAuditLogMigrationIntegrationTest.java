package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.AfterEach;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class FinanceAuditLogMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM finance_audit_log WHERE id LIKE 'test-%'");
    }

    @Test
    @DisplayName("finance_audit_log table exists with all expected columns and types")
    void testTableAndColumnsExist() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'finance_audit_log' " +
                "ORDER BY ordinal_position"
        );

        assertThat(columns).hasSize(12);

        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            columnTypes.put((String) col.get("column_name"), (String) col.get("data_type"));
        }

        assertThat(columnTypes).containsEntry("id", "character varying");
        assertThat(columnTypes).containsEntry("correlation_id", "character varying");
        assertThat(columnTypes).containsEntry("operation_type", "character varying");
        assertThat(columnTypes).containsEntry("status", "character varying");
        assertThat(columnTypes).containsEntry("entity_id", "character varying");
        assertThat(columnTypes).containsEntry("entity_type", "character varying");
        assertThat(columnTypes).containsEntry("group_id", "character varying");
        assertThat(columnTypes).containsEntry("actor_user_id", "character varying");
        assertThat(columnTypes).containsEntry("amount_cents", "bigint");
        assertThat(columnTypes).containsEntry("currency", "character varying");
        assertThat(columnTypes).containsEntry("metadata", "jsonb");
        assertThat(columnTypes).containsEntry("created_at", "timestamp with time zone");
    }

    @Test
    @DisplayName("NOT NULL constraints are correctly applied")
    void testNotNullConstraints() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'finance_audit_log' ORDER BY ordinal_position"
        );

        Map<String, String> nullability = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            nullability.put((String) col.get("column_name"), (String) col.get("is_nullable"));
        }

        assertThat(nullability).containsEntry("id", "NO");
        assertThat(nullability).containsEntry("correlation_id", "NO");
        assertThat(nullability).containsEntry("operation_type", "NO");
        assertThat(nullability).containsEntry("status", "NO");
        assertThat(nullability).containsEntry("entity_type", "NO");
        assertThat(nullability).containsEntry("group_id", "NO");
        assertThat(nullability).containsEntry("actor_user_id", "NO");
        assertThat(nullability).containsEntry("created_at", "NO");

        assertThat(nullability).containsEntry("entity_id", "YES");
        assertThat(nullability).containsEntry("amount_cents", "YES");
        assertThat(nullability).containsEntry("currency", "YES");
        assertThat(nullability).containsEntry("metadata", "YES");
    }

    @Test
    @DisplayName("primary key exists on id column")
    void testPrimaryKey() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'finance_audit_log' AND constraint_type = 'PRIMARY KEY'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("all four indexes exist")
    void testIndexesExist() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes " +
                "WHERE tablename = 'finance_audit_log' AND indexname LIKE 'idx_fal_%' " +
                "ORDER BY indexname",
                String.class
        );

        assertThat(indexes).containsExactly(
                "idx_fal_actor_time",
                "idx_fal_correlation_id",
                "idx_fal_entity",
                "idx_fal_group_time"
        );
    }

    @Test
    @DisplayName("valid row can be inserted")
    void testValidInsert() {
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                "entity_id, entity_type, group_id, actor_user_id, amount_cents, currency) " +
                "VALUES ('test-fal-1', 'corr-1', 'expense_create', 'SUCCESS', " +
                "'exp-1', 'expense', 'grp-1', 'usr-1', 1500, 'USD')"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_audit_log WHERE id = 'test-fal-1'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STARTED", "SUCCESS", "FAILURE"})
    @DisplayName("CHECK constraint allows valid status values")
    void testValidStatuses(String status) {
        String id = "test-fal-s-" + status.toLowerCase();
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                "entity_type, group_id, actor_user_id) " +
                "VALUES (?, 'corr-1', 'expense_create', ?, 'expense', 'grp-1', 'usr-1')",
                id, status
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_audit_log WHERE id = ?", Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid status")
    void testInvalidStatus() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                        "entity_type, group_id, actor_user_id) " +
                        "VALUES ('test-fal-bad', 'corr-1', 'expense_create', 'PENDING', 'expense', 'grp-1', 'usr-1')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expense", "settlement"})
    @DisplayName("CHECK constraint allows valid entity_type values")
    void testValidEntityTypes(String entityType) {
        String id = "test-fal-et-" + entityType;
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                "entity_type, group_id, actor_user_id) " +
                "VALUES (?, 'corr-1', ?, 'SUCCESS', ?, 'grp-1', 'usr-1')",
                id, entityType + "_create", entityType
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_audit_log WHERE id = ?", Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid entity_type")
    void testInvalidEntityType() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                        "entity_type, group_id, actor_user_id) " +
                        "VALUES ('test-fal-bad', 'corr-1', 'expense_create', 'SUCCESS', 'payment', 'grp-1', 'usr-1')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "expense_create", "expense_update", "expense_delete",
            "settlement_create", "settlement_update", "settlement_delete"
    })
    @DisplayName("CHECK constraint allows valid operation_type values")
    void testValidOperationTypes(String opType) {
        String id = "test-fal-op-" + opType.replace("_", "-");
        String entityType = opType.startsWith("expense") ? "expense" : "settlement";
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                "entity_type, group_id, actor_user_id) " +
                "VALUES (?, 'corr-1', ?, 'SUCCESS', ?, 'grp-1', 'usr-1')",
                id, opType, entityType
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_audit_log WHERE id = ?", Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid operation_type")
    void testInvalidOperationType() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                        "entity_type, group_id, actor_user_id) " +
                        "VALUES ('test-fal-bad', 'corr-1', 'expense_archive', 'SUCCESS', 'expense', 'grp-1', 'usr-1')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid currency format")
    void testInvalidCurrency() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                        "entity_type, group_id, actor_user_id, currency) " +
                        "VALUES ('test-fal-bad', 'corr-1', 'expense_create', 'SUCCESS', 'expense', 'grp-1', 'usr-1', 'us')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("NULL currency is allowed")
    void testNullCurrencyAllowed() {
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (id, correlation_id, operation_type, status, " +
                "entity_type, group_id, actor_user_id, currency) " +
                "VALUES ('test-fal-nc', 'corr-1', 'expense_delete', 'SUCCESS', 'expense', 'grp-1', 'usr-1', NULL)"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_audit_log WHERE id = 'test-fal-nc'", Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("created_at defaults to NOW()")
    void testCreatedAtDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'finance_audit_log' AND column_name = 'created_at'",
                String.class
        );
        assertThat(defaultValue).isEqualToIgnoringCase("now()");
    }

    @Test
    @DisplayName("table has no soft-delete or version columns (append-only design)")
    void testNoSoftDeleteOrVersion() {
        List<String> columnNames = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'finance_audit_log'",
                String.class
        );

        assertThat(columnNames).doesNotContain("is_deleted", "version", "updated_at");
    }
}
