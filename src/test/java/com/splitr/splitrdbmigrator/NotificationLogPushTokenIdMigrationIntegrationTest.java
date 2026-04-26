package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-postgres")
class NotificationLogPushTokenIdMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM notification_log WHERE id LIKE 'test-nl-%'");
        jdbcTemplate.update("DELETE FROM users WHERE id LIKE 'test-nl-usr-%'");
    }

    @Test
    @DisplayName("push_token_id column exists on notification_log")
    void testColumnExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'notification_log' AND column_name = 'push_token_id'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("push_token_id is VARCHAR(60)")
    void testColumnType() {
        Map<String, Object> meta = jdbcTemplate.queryForMap(
                "SELECT data_type, character_maximum_length FROM information_schema.columns " +
                "WHERE table_name = 'notification_log' AND column_name = 'push_token_id'"
        );
        assertThat(meta.get("data_type")).isEqualTo("character varying");
        assertThat(((Number) meta.get("character_maximum_length")).intValue()).isEqualTo(60);
    }

    @Test
    @DisplayName("push_token_id is nullable")
    void testColumnIsNullable() {
        String isNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'notification_log' AND column_name = 'push_token_id'",
                String.class
        );
        assertThat(isNullable).isEqualTo("YES");
    }

    @Test
    @DisplayName("rows can be inserted with NULL push_token_id (pre-H4 fallback)")
    void testNullValueAccepted() {
        insertTestUser("test-nl-usr-1");
        insertNotificationLog("test-nl-1", "test-nl-usr-1", null);

        String value = jdbcTemplate.queryForObject(
                "SELECT push_token_id FROM notification_log WHERE id = 'test-nl-1'",
                String.class
        );
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("rows can be inserted with a push_token_id value")
    void testNonNullValuePersisted() {
        insertTestUser("test-nl-usr-2");
        insertNotificationLog("test-nl-2", "test-nl-usr-2", "push-token-abc-123");

        String value = jdbcTemplate.queryForObject(
                "SELECT push_token_id FROM notification_log WHERE id = 'test-nl-2'",
                String.class
        );
        assertThat(value).isEqualTo("push-token-abc-123");
    }

    private void insertTestUser(String id) {
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING",
                id, "clerk-" + id, id + "@test.com", "Test User"
        );
    }

    private void insertNotificationLog(String id, String userId, String pushTokenId) {
        jdbcTemplate.update(
                "INSERT INTO notification_log (id, user_id, notification_type, title, body, created_at, push_token_id) " +
                "VALUES (?, ?, 'test_type', 'Test Title', 'Test body', ?, ?)",
                id, userId, OffsetDateTime.now(), pushTokenId
        );
    }
}
