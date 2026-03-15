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
class CompositeIndexesMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testCompositeAndPartialIndexesExist() {
        List<String> expectedIndexes = List.of(
                "idx_exchange_rates_lookup",
                "idx_expenses_group_active",
                "idx_settlements_group_active",
                "idx_group_members_group_active",
                "idx_group_members_user_active",
                "idx_push_tokens_user_active",
                "uq_group_members_group_user",
                "uq_group_members_group_guest",
                "idx_guest_users_email",
                "idx_notification_log_pending",
                "idx_notification_log_sent_tickets",
                "idx_activity_log_group_created"
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
