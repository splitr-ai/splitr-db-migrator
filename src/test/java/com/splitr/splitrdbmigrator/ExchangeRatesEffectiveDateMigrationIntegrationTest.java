package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class ExchangeRatesEffectiveDateMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("effective_date column type is DATE (not TIMESTAMPTZ)")
    void testEffectiveDateIsDate() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = 'exchange_rates' AND column_name = 'effective_date'",
                String.class
        );
        assertThat(dataType).isEqualTo("date");
    }

    @Test
    @DisplayName("effective_date column is NOT NULL")
    void testEffectiveDateNotNull() {
        String isNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'exchange_rates' AND column_name = 'effective_date'",
                String.class
        );
        assertThat(isNullable).isEqualTo("NO");
    }

    @Test
    @DisplayName("unique constraint on (base_currency, target_currency, effective_date) still exists")
    void testUniqueConstraintIntact() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'exchange_rates' AND constraint_type = 'UNIQUE'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("lookup index on exchange_rates still exists")
    void testLookupIndexIntact() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'exchange_rates' AND indexname = 'idx_exchange_rates_lookup'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
