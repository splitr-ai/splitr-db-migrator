package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.DisplayName;
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
class UsersPhoneIndexMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("idx_users_phone index exists on users table")
    void testIndexExists() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes " +
                "WHERE tablename = 'users' AND indexname = 'idx_users_phone'",
                String.class
        );
        assertThat(indexes).containsExactly("idx_users_phone");
    }

    @Test
    @DisplayName("idx_users_phone is a partial index with WHERE phone IS NOT NULL")
    void testIndexIsPartial() {
        String indexDef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes " +
                "WHERE tablename = 'users' AND indexname = 'idx_users_phone'",
                String.class
        );
        assertThat(indexDef).containsIgnoringCase("WHERE");
        assertThat(indexDef).containsIgnoringCase("phone IS NOT NULL");
    }

    @Test
    @DisplayName("idx_users_phone indexes the phone column")
    void testIndexColumn() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT a.attname FROM pg_index i " +
                "JOIN pg_class c ON c.oid = i.indexrelid " +
                "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                "WHERE c.relname = 'idx_users_phone'",
                String.class
        );
        assertThat(columns).containsExactly("phone");
    }

    @Test
    @DisplayName("index is not unique")
    void testIndexIsNotUnique() {
        Boolean isUnique = jdbcTemplate.queryForObject(
                "SELECT i.indisunique FROM pg_index i " +
                "JOIN pg_class c ON c.oid = i.indexrelid " +
                "WHERE c.relname = 'idx_users_phone'",
                Boolean.class
        );
        assertThat(isUnique).isFalse();
    }
}
