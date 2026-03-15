package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class ActivityLogCheckConstraintIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testNewActivityTypesAreAllowed() {
        String userId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        
        // Insert a user first
        jdbcTemplate.execute(String.format(
            "INSERT INTO users (id, email, name, clerk_id) VALUES ('%s', '%s@example.com', 'Test User', '%s')",
            userId, userId, userId
        ));
        
        // Insert a group
        jdbcTemplate.execute(String.format(
            "INSERT INTO groups (id, name, created_by) VALUES ('%s', 'Test Group', '%s')",
            groupId, userId
        ));

        String[] newTypes = {
            "settlement_updated",
            "group_deleted",
            "member_joined_via_invite",
            "invite_code_regenerated",
            "expense_comment_added"
        };

        for (String type : newTypes) {
            String id = UUID.randomUUID().toString();
            jdbcTemplate.execute(String.format(
                "INSERT INTO activity_log (id, group_id, activity_type) VALUES ('%s', '%s', '%s')",
                id, groupId, type
            ));
        }

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM activity_log WHERE group_id = ?",
            Integer.class,
            groupId
        );
        assertThat(count).isEqualTo(newTypes.length);
    }

    @Test
    void testInvalidActivityTypeIsForbidden() {
        String userId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        
        // Insert a user first
        jdbcTemplate.execute(String.format(
            "INSERT INTO users (id, email, name, clerk_id) VALUES ('%s', '%s@example.com', 'Test User', '%s')",
            userId, userId, userId
        ));
        
        // Insert a group
        jdbcTemplate.execute(String.format(
            "INSERT INTO groups (id, name, created_by) VALUES ('%s', 'Test Group', '%s')",
            groupId, userId
        ));

        String invalidType = "invalid_activity_type";
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> {
            jdbcTemplate.execute(String.format(
                "INSERT INTO activity_log (id, group_id, activity_type) VALUES ('%s', '%s', '%s')",
                id, groupId, invalidType
            ));
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_activity_log_type");
    }
}
