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
class RecurringExpenseTemplatesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM recurring_expense_templates WHERE id LIKE 'test-%'");
    }

    @Test
    @DisplayName("recurring_expense_templates table exists with all expected columns and types")
    void testTableAndColumnsExist() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' " +
                "ORDER BY ordinal_position"
        );

        assertThat(columns).hasSize(22);

        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            columnTypes.put((String) col.get("column_name"), (String) col.get("data_type"));
        }

        assertThat(columnTypes).containsEntry("id", "character varying");
        assertThat(columnTypes).containsEntry("group_id", "character varying");
        assertThat(columnTypes).containsEntry("created_by", "character varying");
        assertThat(columnTypes).containsEntry("description", "character varying");
        assertThat(columnTypes).containsEntry("total_amount", "bigint");
        assertThat(columnTypes).containsEntry("currency", "character varying");
        assertThat(columnTypes).containsEntry("category_id", "character varying");
        assertThat(columnTypes).containsEntry("split_type", "character varying");
        assertThat(columnTypes).containsEntry("notes", "text");
        assertThat(columnTypes).containsEntry("frequency", "character varying");
        assertThat(columnTypes).containsEntry("anchor_date", "date");
        assertThat(columnTypes).containsEntry("next_run_date", "date");
        assertThat(columnTypes).containsEntry("end_date", "date");
        assertThat(columnTypes).containsEntry("status", "character varying");
        assertThat(columnTypes).containsEntry("last_created_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("total_occurrences", "integer");
        assertThat(columnTypes).containsEntry("payers_json", "jsonb");
        assertThat(columnTypes).containsEntry("splits_json", "jsonb");
        assertThat(columnTypes).containsEntry("created_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("updated_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("is_deleted", "boolean");
        assertThat(columnTypes).containsEntry("version", "integer");
    }

    @Test
    @DisplayName("NOT NULL constraints are correctly applied")
    void testNotNullConstraints() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' ORDER BY ordinal_position"
        );

        Map<String, String> nullability = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            nullability.put((String) col.get("column_name"), (String) col.get("is_nullable"));
        }

        // NOT NULL columns
        assertThat(nullability).containsEntry("id", "NO");
        assertThat(nullability).containsEntry("group_id", "NO");
        assertThat(nullability).containsEntry("created_by", "NO");
        assertThat(nullability).containsEntry("description", "NO");
        assertThat(nullability).containsEntry("total_amount", "NO");
        assertThat(nullability).containsEntry("currency", "NO");
        assertThat(nullability).containsEntry("split_type", "NO");
        assertThat(nullability).containsEntry("frequency", "NO");
        assertThat(nullability).containsEntry("anchor_date", "NO");
        assertThat(nullability).containsEntry("next_run_date", "NO");
        assertThat(nullability).containsEntry("status", "NO");
        assertThat(nullability).containsEntry("total_occurrences", "NO");
        assertThat(nullability).containsEntry("payers_json", "NO");
        assertThat(nullability).containsEntry("splits_json", "NO");
        assertThat(nullability).containsEntry("created_at", "NO");
        assertThat(nullability).containsEntry("updated_at", "NO");
        assertThat(nullability).containsEntry("is_deleted", "NO");
        assertThat(nullability).containsEntry("version", "NO");

        // Nullable columns
        assertThat(nullability).containsEntry("category_id", "YES");
        assertThat(nullability).containsEntry("notes", "YES");
        assertThat(nullability).containsEntry("end_date", "YES");
        assertThat(nullability).containsEntry("last_created_at", "YES");
    }

    @Test
    @DisplayName("status defaults to 'active'")
    void testStatusDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' AND column_name = 'status'",
                String.class
        );
        assertThat(defaultValue).contains("active");
    }

    @Test
    @DisplayName("total_occurrences defaults to 0")
    void testTotalOccurrencesDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' AND column_name = 'total_occurrences'",
                String.class
        );
        assertThat(defaultValue).isEqualTo("0");
    }

    @Test
    @DisplayName("is_deleted defaults to FALSE")
    void testIsDeletedDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' AND column_name = 'is_deleted'",
                String.class
        );
        assertThat(defaultValue).isEqualTo("false");
    }

    @Test
    @DisplayName("version defaults to 0")
    void testVersionDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'recurring_expense_templates' AND column_name = 'version'",
                String.class
        );
        assertThat(defaultValue).isEqualTo("0");
    }

    @Test
    @DisplayName("primary key exists on id column")
    void testPrimaryKey() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'recurring_expense_templates' AND constraint_type = 'PRIMARY KEY'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("foreign keys exist for group_id, created_by, and category_id")
    void testForeignKeys() {
        List<Map<String, Object>> fks = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_name = 'recurring_expense_templates' AND constraint_type = 'FOREIGN KEY' " +
                "ORDER BY constraint_name"
        );
        assertThat(fks).hasSize(3);
    }

    @Test
    @DisplayName("idx_recur_group_id index exists")
    void testGroupIdIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'recurring_expense_templates' AND indexname = 'idx_recur_group_id'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("idx_recur_created_by index exists")
    void testCreatedByIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'recurring_expense_templates' AND indexname = 'idx_recur_created_by'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("idx_recur_next_run_date partial index exists")
    void testNextRunDatePartialIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'recurring_expense_templates' AND indexname = 'idx_recur_next_run_date'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid currency")
    void testInvalidCurrency() {
        assertThatThrownBy(() -> insertTemplate("test-bad-curr", "us", "equal", "monthly", "active"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects non-positive amount")
    void testNonPositiveAmount() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO recurring_expense_templates " +
                        "(id, group_id, created_by, description, total_amount, currency, split_type, " +
                        "frequency, anchor_date, next_run_date, payers_json, splits_json) " +
                        "SELECT 'test-bad-amt', g.id, u.id, 'Test', 0, 'USD', 'equal', " +
                        "'monthly', '2026-04-01', '2026-04-01', '{}'::jsonb, '{}'::jsonb " +
                        "FROM groups g, users u LIMIT 1"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid split_type")
    void testInvalidSplitType() {
        assertThatThrownBy(() -> insertTemplate("test-bad-split", "USD", "custom", "monthly", "active"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"equal", "exact", "percentage", "shares"})
    @DisplayName("CHECK constraint allows valid split_type values")
    void testValidSplitTypes(String splitType) {
        String id = "test-split-" + splitType;
        insertTemplate(id, "USD", splitType, "monthly", "active");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recurring_expense_templates WHERE id = ?",
                Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid frequency")
    void testInvalidFrequency() {
        assertThatThrownBy(() -> insertTemplate("test-bad-freq", "USD", "equal", "hourly", "active"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly", "biweekly", "monthly", "yearly"})
    @DisplayName("CHECK constraint allows valid frequency values")
    void testValidFrequencies(String frequency) {
        String id = "test-freq-" + frequency;
        insertTemplate(id, "USD", "equal", frequency, "active");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recurring_expense_templates WHERE id = ?",
                Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid status")
    void testInvalidStatus() {
        assertThatThrownBy(() -> insertTemplate("test-bad-stat", "USD", "equal", "monthly", "deleted"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "paused", "expired", "cancelled"})
    @DisplayName("CHECK constraint allows valid status values")
    void testValidStatuses(String status) {
        String id = "test-status-" + status;
        insertTemplate(id, "USD", "equal", "monthly", status);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recurring_expense_templates WHERE id = ?",
                Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects negative total_occurrences")
    void testNegativeOccurrences() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO recurring_expense_templates " +
                        "(id, group_id, created_by, description, total_amount, currency, split_type, " +
                        "frequency, anchor_date, next_run_date, total_occurrences, payers_json, splits_json) " +
                        "SELECT 'test-bad-occ', g.id, u.id, 'Test', 1000, 'USD', 'equal', " +
                        "'monthly', '2026-04-01', '2026-04-01', -1, '{}'::jsonb, '{}'::jsonb " +
                        "FROM groups g, users u LIMIT 1"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertTemplate(String id, String currency, String splitType, String frequency, String status) {
        jdbcTemplate.update(
                "INSERT INTO recurring_expense_templates " +
                "(id, group_id, created_by, description, total_amount, currency, split_type, " +
                "frequency, anchor_date, next_run_date, status, payers_json, splits_json) " +
                "SELECT ?, g.id, u.id, 'Test Recurring', 1000, ?, ?, " +
                "?, '2026-04-01', '2026-04-01', ?, '{}'::jsonb, '{}'::jsonb " +
                "FROM groups g, users u LIMIT 1",
                id, currency, splitType, frequency, status
        );
    }
}
