package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class TimestamptzMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testColumnsAreTimestamptz() {
        List<ColumnInfo> columnsToVerify = List.of(
                new ColumnInfo("users", "created_at"),
                new ColumnInfo("users", "updated_at"),
                new ColumnInfo("groups", "created_at"),
                new ColumnInfo("groups", "updated_at"),
                new ColumnInfo("groups", "invite_code_expires_at"),
                new ColumnInfo("groups", "archived_at"),
                new ColumnInfo("expenses", "created_at"),
                new ColumnInfo("expenses", "updated_at"),
                new ColumnInfo("settlements", "created_at"),
                new ColumnInfo("settlements", "updated_at"),
                new ColumnInfo("group_members", "created_at"),
                new ColumnInfo("group_members", "updated_at"),
                new ColumnInfo("group_members", "joined_at"),
                new ColumnInfo("group_members", "left_at"),
                new ColumnInfo("guest_users", "created_at"),
                new ColumnInfo("guest_users", "updated_at"),
                new ColumnInfo("guest_users", "token_expires_at"),
                new ColumnInfo("push_tokens", "created_at"),
                new ColumnInfo("push_tokens", "updated_at"),
                new ColumnInfo("push_tokens", "last_used_at"),
                new ColumnInfo("notification_log", "created_at"),
                new ColumnInfo("notification_log", "delivered_at"),
                new ColumnInfo("activity_log", "created_at"),
                // effective_date was reverted to DATE by V33 — verified in ExchangeRatesEffectiveDateMigrationIntegrationTest
                new ColumnInfo("exchange_rates", "fetched_at"),
                new ColumnInfo("categories", "created_at")
        );

        for (ColumnInfo info : columnsToVerify) {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                    String.class,
                    info.tableName,
                    info.columnName
            );
            assertThat(dataType)
                    .as("Column %s.%s should be timestamp with time zone", info.tableName, info.columnName)
                    .isEqualTo("timestamp with time zone");
        }
    }

    private static class ColumnInfo {
        final String tableName;
        final String columnName;

        ColumnInfo(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }
    }
}
