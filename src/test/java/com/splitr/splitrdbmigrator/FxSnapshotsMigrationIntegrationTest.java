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
class FxSnapshotsMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM fx_snapshots WHERE id LIKE 'test-%'");
    }

    @Test
    @DisplayName("fx_snapshots table exists with all expected columns and types")
    void testTableAndColumnsExist() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'fx_snapshots' " +
                "ORDER BY ordinal_position"
        );

        assertThat(columns).hasSize(12);

        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            columnTypes.put((String) col.get("column_name"), (String) col.get("data_type"));
        }

        assertThat(columnTypes).containsEntry("id", "character varying");
        assertThat(columnTypes).containsEntry("base_currency", "character varying");
        assertThat(columnTypes).containsEntry("quote_currency", "character varying");
        assertThat(columnTypes).containsEntry("rate", "numeric");
        assertThat(columnTypes).containsEntry("rate_source", "character varying");
        assertThat(columnTypes).containsEntry("quoted_at", "timestamp with time zone");
        assertThat(columnTypes).containsEntry("rounding_mode", "character varying");
        assertThat(columnTypes).containsEntry("scale", "integer");
        assertThat(columnTypes).containsEntry("original_amount", "bigint");
        assertThat(columnTypes).containsEntry("converted_amount", "bigint");
        assertThat(columnTypes).containsEntry("correlation_id", "character varying");
        assertThat(columnTypes).containsEntry("created_at", "timestamp with time zone");
    }

    @Test
    @DisplayName("NOT NULL constraints are correctly applied")
    void testNotNullConstraints() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'fx_snapshots' ORDER BY ordinal_position"
        );

        Map<String, String> nullability = new LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            nullability.put((String) col.get("column_name"), (String) col.get("is_nullable"));
        }

        assertThat(nullability.values()).allMatch(v -> v.equals("NO"));
    }

    @Test
    @DisplayName("rounding_mode defaults to HALF_EVEN")
    void testRoundingModeDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'fx_snapshots' AND column_name = 'rounding_mode'",
                String.class
        );
        assertThat(defaultValue).contains("HALF_EVEN");
    }

    @Test
    @DisplayName("scale defaults to 0")
    void testScaleDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'fx_snapshots' AND column_name = 'scale'",
                String.class
        );
        assertThat(defaultValue).isEqualTo("0");
    }

    @Test
    @DisplayName("created_at defaults to NOW()")
    void testCreatedAtDefault() {
        String defaultValue = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'fx_snapshots' AND column_name = 'created_at'",
                String.class
        );
        assertThat(defaultValue).isEqualToIgnoringCase("now()");
    }

    @Test
    @DisplayName("primary key exists on id column")
    void testPrimaryKey() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'fx_snapshots' AND constraint_type = 'PRIMARY KEY'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("idx_fx_snapshots_correlation index exists")
    void testCorrelationIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'fx_snapshots' AND indexname = 'idx_fx_snapshots_correlation'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("valid row can be inserted")
    void testValidInsert() {
        jdbcTemplate.update(
                "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                "quoted_at, original_amount, converted_amount, correlation_id) " +
                "VALUES ('test-fx-1', 'USD', 'EUR', 0.92345678, 'ECB', NOW(), 1000, 923, 'test-corr-1')"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fx_snapshots WHERE id = 'test-fx-1'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid base_currency")
    void testInvalidBaseCurrency() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'us', 'EUR', 0.92, 'ECB', NOW(), 1000, 920, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid quote_currency")
    void testInvalidQuoteCurrency() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'USD', '12', 0.92, 'ECB', NOW(), 1000, 920, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects zero or negative rate")
    void testNonPositiveRate() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'USD', 'EUR', 0, 'ECB', NOW(), 1000, 920, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects non-positive original_amount")
    void testNonPositiveOriginalAmount() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'USD', 'EUR', 0.92, 'ECB', NOW(), 0, 920, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects non-positive converted_amount")
    void testNonPositiveConvertedAmount() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'USD', 'EUR', 0.92, 'ECB', NOW(), 1000, -1, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CHECK constraint rejects invalid rounding_mode")
    void testInvalidRoundingMode() {
        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                        "quoted_at, rounding_mode, original_amount, converted_amount, correlation_id) " +
                        "VALUES ('test-fx-bad', 'USD', 'EUR', 0.92, 'ECB', NOW(), 'INVALID', 1000, 920, 'test-corr-bad')"
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HALF_EVEN", "HALF_UP", "HALF_DOWN", "CEILING", "FLOOR"})
    @DisplayName("CHECK constraint allows valid rounding_mode values")
    void testValidRoundingModes(String mode) {
        String id = "test-fx-rm-" + mode.toLowerCase();
        jdbcTemplate.update(
                "INSERT INTO fx_snapshots (id, base_currency, quote_currency, rate, rate_source, " +
                "quoted_at, rounding_mode, original_amount, converted_amount, correlation_id) " +
                "VALUES (?, 'USD', 'EUR', 0.92, 'ECB', NOW(), ?, 1000, 920, ?)",
                id, mode, "test-corr-" + mode.toLowerCase()
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fx_snapshots WHERE id = ?",
                Integer.class, id
        );
        assertThat(count).isEqualTo(1);
    }
}
