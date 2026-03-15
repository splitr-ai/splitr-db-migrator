package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class FkIndexesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testFkIndexesExist() {
        List<String> expectedIndexes = List.of(
                "idx_groups_created_by",
                "idx_users_referred_by",
                "idx_categories_created_by",
                "idx_categories_parent_id",
                "idx_settlements_payer_guest_id",
                "idx_settlements_payee_guest_id",
                "idx_settlements_created_by",
                "idx_activity_log_expense_id",
                "idx_activity_log_settlement_id",
                "idx_activity_log_actor_guest_id",
                "idx_expenses_created_by_guest",
                "idx_notification_log_group_id"
        );

        for (String indexName : expectedIndexes) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE indexname = ?",
                    Integer.class,
                    indexName
            );
            assertThat(count).as("Index %s should exist", indexName).isEqualTo(1);
        }
    }
}
