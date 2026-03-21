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
class LastDigestSentAtMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testLastDigestSentAtColumnExistsAndIsTimestamptz() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'last_digest_sent_at'",
                String.class
        );
        assertThat(dataType)
                .as("Column users.last_digest_sent_at should be timestamp with time zone")
                .isEqualTo("timestamp with time zone");
    }

    @Test
    void testLastDigestSentAtColumnIsNullable() {
        String isNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'last_digest_sent_at'",
                String.class
        );
        assertThat(isNullable)
                .as("Column users.last_digest_sent_at should be nullable")
                .isEqualTo("YES");
    }

    @Test
    void testLastDigestSentAtComment() {
        String comment = jdbcTemplate.queryForObject(
                "SELECT pg_catalog.col_description(c.oid, cols.ordinal_position::int) " +
                        "FROM pg_catalog.pg_class c " +
                        "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                        "JOIN information_schema.columns cols ON cols.table_name = c.relname " +
                        "WHERE c.relname = 'users' AND cols.column_name = 'last_digest_sent_at' AND n.nspname = 'public'",
                String.class
        );
        assertThat(comment).isEqualTo("Timestamp of the last email digest sent to this user");
    }
}
