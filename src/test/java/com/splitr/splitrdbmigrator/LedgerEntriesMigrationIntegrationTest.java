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
class LedgerEntriesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM ledger_entries WHERE id LIKE 'test-led-%'");
    }

    @Test
    @DisplayName("ledger_entries table exists with all expected columns and types")
    void testTableAndColumnsExist() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable " +
                        "FROM information_schema.columns " +
                        "WHERE table_name = 'ledger_entries' " +
                        "ORDER BY ordinal_position"
        );

        assertThat(columns).hasSize(12);

        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            columnTypes.put((String) col.get("column_name"), (String) col.get("data_type"));
        }

        assertThat(columnTypes).containsEntry("id", "character varying");
        assertThat(columnTypes).containsEntry("correlation_id", "character varying");
        assertThat(columnTypes).containsEntry("group_id", "character varying");
        assertThat(columnTypes).containsEntry("member_key", "character varying");
        assertThat(columnTypes).containsEntry("amount", "bigint");
        assertThat(columnTypes).containsEntry("currency", "character varying");
        assertThat(columnTypes).containsEntry("operation_type", "character varying");
        assertThat(columnTypes).containsEntry("source_type", "character varying");
        assertThat(columnTypes).containsEntry("source_id", "character varying");
        assertThat(columnTypes).containsEntry("source_version", "integer");
        assertThat(columnTypes).containsEntry("description", "text");
        assertThat(columnTypes).containsEntry("created_at", "timestamp with time zone");
    }

    @Test
    @DisplayName("NOT NULL constraints are correctly applied")
    void testNotNullConstraints() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                        "WHERE table_name = 'ledger_entries' ORDER BY ordinal_position"
        );

        Map<String, String> nullability = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            nullability.put((String) col.get("column_name"), (String) col.get("is_nullable"));
        }

        assertThat(nullability).containsEntry("id", "NO");
        assertThat(nullability).containsEntry("correlation_id", "NO");
        assertThat(nullability).containsEntry("group_id", "NO");
        assertThat(nullability).containsEntry("member_key", "NO");
        assertThat(nullability).containsEntry("amount", "NO");
        assertThat(nullability).containsEntry("currency", "NO");
        assertThat(nullability).containsEntry("operation_type", "NO");
        assertThat(nullability).containsEntry("source_type", "NO");
        assertThat(nullability).containsEntry("source_id", "NO");
        assertThat(nullability).containsEntry("source_version", "NO");
        assertThat(nullability).containsEntry("created_at", "NO");
        assertThat(nullability).containsEntry("description", "YES");
    }

    @Test
    @DisplayName("primary key exists on id column")
    void testPrimaryKey() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                        "WHERE table_name = 'ledger_entries' AND constraint_type = 'PRIMARY KEY'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("all five indexes exist")
    void testIndexesExist() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes " +
                        "WHERE tablename = 'ledger_entries' AND indexname LIKE 'idx_ledger_%' " +
                        "ORDER BY indexname",
                String.class
        );

        assertThat(indexes).containsExactly(
                "idx_ledger_correlation",
                "idx_ledger_group_currency",
                "idx_ledger_group_time",
                "idx_ledger_member_group",
                "idx_ledger_source"
        );
    }

    @Test
    @DisplayName("valid row can be inserted")
    void testValidInsert() {
        jdbcTemplate.update(
                "INSERT INTO ledger_entries (" +
                        "id, correlation_id, group_id, member_key, amount, currency, " +
                        "operation_type, source_type, source_id, source_version, description" +
                        ") VALUES (" +
                        "'test-led-1', 'corr-1', 'grp-1', 'user:usr-1', 500, 'USD', " +
                        "'RECORD', 'EXPENSE', 'exp-1', 1, 'dinner'" +
                        ")"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE id = 'test-led-1'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECORD", "REVERSAL"})
    @DisplayName("CHECK constraint allows valid operation_type values")
    void testValidOperationTypes(String operationType) {
        String id = "test-led-op-" + operationType.toLowerCase();
        jdbcTemplate.update(
                "INSERT INTO ledger_entries (" +
                        "id, correlation_id, group_id, member_key, amount, currency, " +
                        "operation_type, source_type, source_id, source_version" +
                        ") VALUES (?, 'corr-1', 'grp-1', 'user:usr-1', 100, 'USD', ?, 'EXPENSE', 'exp-1', 1)",
                id, operationType
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE id = ?", Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid operation_type")
    void testInvalidOperationType() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO ledger_entries (" +
                                "id, correlation_id, group_id, member_key, amount, currency, " +
                                "operation_type, source_type, source_id, source_version" +
                                ") VALUES (" +
                                "'test-led-bad-op', 'corr-1', 'grp-1', 'user:usr-1', 100, 'USD', " +
                                "'UPDATE', 'EXPENSE', 'exp-1', 1" +
                                ")"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPENSE", "SETTLEMENT"})
    @DisplayName("CHECK constraint allows valid source_type values")
    void testValidSourceTypes(String sourceType) {
        String id = "test-led-src-" + sourceType.toLowerCase();
        jdbcTemplate.update(
                "INSERT INTO ledger_entries (" +
                        "id, correlation_id, group_id, member_key, amount, currency, " +
                        "operation_type, source_type, source_id, source_version" +
                        ") VALUES (?, 'corr-1', 'grp-1', 'user:usr-1', 100, 'USD', 'RECORD', ?, 'src-1', 1)",
                id, sourceType
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ledger_entries WHERE id = ?", Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid source_type")
    void testInvalidSourceType() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO ledger_entries (" +
                                "id, correlation_id, group_id, member_key, amount, currency, " +
                                "operation_type, source_type, source_id, source_version" +
                                ") VALUES (" +
                                "'test-led-bad-src', 'corr-1', 'grp-1', 'user:usr-1', 100, 'USD', " +
                                "'RECORD', 'PAYMENT', 'src-1', 1" +
                                ")"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects zero amount")
    void testZeroAmountRejected() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO ledger_entries (" +
                                "id, correlation_id, group_id, member_key, amount, currency, " +
                                "operation_type, source_type, source_id, source_version" +
                                ") VALUES (" +
                                "'test-led-zero', 'corr-1', 'grp-1', 'user:usr-1', 0, 'USD', " +
                                "'RECORD', 'EXPENSE', 'exp-1', 1" +
                                ")"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid currency format")
    void testInvalidCurrencyRejected() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO ledger_entries (" +
                                "id, correlation_id, group_id, member_key, amount, currency, " +
                                "operation_type, source_type, source_id, source_version" +
                                ") VALUES (" +
                                "'test-led-bad-cur', 'corr-1', 'grp-1', 'user:usr-1', 100, 'us', " +
                                "'RECORD', 'EXPENSE', 'exp-1', 1" +
                                ")"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
