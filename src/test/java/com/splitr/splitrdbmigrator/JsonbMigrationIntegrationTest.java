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
class JsonbMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testColumnsAreJsonb() {
        List<ColumnInfo> columnsToVerify = List.of(
                new ColumnInfo("expenses", "input_metadata"),
                new ColumnInfo("activity_log", "details"),
                new ColumnInfo("notification_log", "data_payload"),
                new ColumnInfo("users", "preferences")
        );

        for (ColumnInfo info : columnsToVerify) {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                    String.class,
                    info.tableName,
                    info.columnName
            );
            // In PostgreSQL, JSONB is reported as "jsonb" in information_schema.columns.data_type
            assertThat(dataType)
                    .as("Column %s.%s should be jsonb", info.tableName, info.columnName)
                    .isEqualTo("jsonb");
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
