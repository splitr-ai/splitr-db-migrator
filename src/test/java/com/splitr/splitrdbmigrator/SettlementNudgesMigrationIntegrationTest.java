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
class SettlementNudgesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testSettlementNudgesTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'settlement_nudges'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testSettlementNudgesIndicesExist() {
        String[] indices = {
                "idx_nudge_group",
                "idx_nudge_debtor",
                "idx_nudge_creditor",
                "idx_nudge_unique_pair"
        };

        for (String indexName : indices) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE indexname = ?",
                    Integer.class,
                    indexName
            );
            assertThat(count).as("Index %s should exist", indexName).isEqualTo(1);
        }
    }
}
