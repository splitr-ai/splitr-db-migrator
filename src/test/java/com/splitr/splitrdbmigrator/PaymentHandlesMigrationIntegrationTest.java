package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class PaymentHandlesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testPaymentHandlesColumnExistsAndIsJsonb() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'payment_handles'",
                String.class
        );
        assertThat(dataType)
                .as("Column users.payment_handles should be jsonb")
                .isEqualTo("jsonb");
    }

    @Test
    void testPaymentHandlesDefaultValue() {
        String columnDefault = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'payment_handles'",
                String.class
        );
        assertThat(columnDefault)
                .as("Column users.payment_handles should have '{}'::jsonb as default")
                .contains("'{}'::jsonb");
    }

    @Test
    void testPaymentHandlesComment() {
        String comment = jdbcTemplate.queryForObject(
                "SELECT pg_catalog.col_description(c.oid, cols.ordinal_position::int) " +
                        "FROM pg_catalog.pg_class c " +
                        "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                        "JOIN information_schema.columns cols ON cols.table_name = c.relname " +
                        "WHERE c.relname = 'users' AND cols.column_name = 'payment_handles' AND n.nspname = 'public'",
                String.class
        );
        assertThat(comment).isEqualTo("User payment app handles (venmo, paypal, upi, etc.) for deep link integration");
    }
}
