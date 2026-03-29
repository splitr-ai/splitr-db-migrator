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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class SettlementsFxColumnsMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("converted_amount column exists on settlements with correct type")
    void testConvertedAmountColumn() {
        Map<String, Object> col = jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'settlements' AND column_name = 'converted_amount'"
        );
        assertThat(col.get("data_type")).isEqualTo("bigint");
        assertThat(col.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    @DisplayName("converted_currency column exists on settlements with correct type")
    void testConvertedCurrencyColumn() {
        Map<String, Object> col = jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable, character_maximum_length FROM information_schema.columns " +
                "WHERE table_name = 'settlements' AND column_name = 'converted_currency'"
        );
        assertThat(col.get("data_type")).isEqualTo("character varying");
        assertThat(col.get("is_nullable")).isEqualTo("YES");
        assertThat(col.get("character_maximum_length")).isEqualTo(3);
    }

    @Test
    @DisplayName("fx_snapshot_id column exists on settlements with correct type")
    void testFxSnapshotIdColumn() {
        Map<String, Object> col = jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable, character_maximum_length FROM information_schema.columns " +
                "WHERE table_name = 'settlements' AND column_name = 'fx_snapshot_id'"
        );
        assertThat(col.get("data_type")).isEqualTo("character varying");
        assertThat(col.get("is_nullable")).isEqualTo("YES");
        assertThat(col.get("character_maximum_length")).isEqualTo(60);
    }

    @Test
    @DisplayName("foreign key from settlements.fx_snapshot_id to fx_snapshots.id exists")
    void testForeignKeyExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'settlements' AND constraint_type = 'FOREIGN KEY' " +
                "AND constraint_name = 'fk_settlements_fx_snapshot'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("CHECK constraint on converted_currency validates currency format")
    void testConvertedCurrencyCheckConstraint() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_name = 'settlements' AND constraint_type = 'CHECK' " +
                "AND constraint_name = 'chk_settlements_converted_currency'"
        );
        assertThat(constraints).hasSize(1);
    }

    @Test
    @DisplayName("all three FX columns are nullable")
    void testAllFxColumnsNullable() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'settlements' AND column_name IN ('converted_amount', 'converted_currency', 'fx_snapshot_id') " +
                "ORDER BY column_name"
        );

        assertThat(columns).hasSize(3);
        for (Map<String, Object> col : columns) {
            assertThat(col.get("is_nullable")).isEqualTo("YES");
        }
    }
}
