package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class GroupMembersUniqueGuestMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM group_members WHERE id LIKE 'test-gm-%'");
        jdbcTemplate.update("DELETE FROM guest_users WHERE id LIKE 'test-gu-%'");
        jdbcTemplate.update("DELETE FROM groups WHERE id LIKE 'test-grp-%'");
        jdbcTemplate.update("DELETE FROM users WHERE id LIKE 'test-usr-%'");
    }

    @Test
    @DisplayName("idx_group_members_unique_guest index exists")
    void testIndexExists() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes " +
                "WHERE tablename = 'group_members' AND indexname = 'idx_group_members_unique_guest'",
                String.class
        );
        assertThat(indexes).containsExactly("idx_group_members_unique_guest");
    }

    @Test
    @DisplayName("index is unique")
    void testIndexIsUnique() {
        Boolean isUnique = jdbcTemplate.queryForObject(
                "SELECT i.indisunique FROM pg_index i " +
                "JOIN pg_class c ON c.oid = i.indexrelid " +
                "WHERE c.relname = 'idx_group_members_unique_guest'",
                Boolean.class
        );
        assertThat(isUnique).isTrue();
    }

    @Test
    @DisplayName("index is partial with WHERE guest_user_id IS NOT NULL AND is_deleted = false")
    void testIndexIsPartial() {
        String indexDef = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes " +
                "WHERE tablename = 'group_members' AND indexname = 'idx_group_members_unique_guest'",
                String.class
        );
        assertThat(indexDef).containsIgnoringCase("WHERE");
        assertThat(indexDef).containsIgnoringCase("guest_user_id IS NOT NULL");
        assertThat(indexDef).containsIgnoringCase("is_deleted");
    }

    @Test
    @DisplayName("index covers group_id and guest_user_id columns")
    void testIndexColumns() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT a.attname FROM pg_index i " +
                "JOIN pg_class c ON c.oid = i.indexrelid " +
                "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                "WHERE c.relname = 'idx_group_members_unique_guest' " +
                "ORDER BY array_position(i.indkey, a.attnum)",
                String.class
        );
        assertThat(columns).containsExactly("group_id", "guest_user_id");
    }

    @Test
    @DisplayName("duplicate guest in same group is rejected when both are active")
    void testDuplicateGuestRejected() {
        insertTestUser("test-usr-1");
        insertTestGroup("test-grp-1", "test-usr-1");
        insertTestGuestUser("test-gu-1");
        insertGroupMember("test-gm-1", "test-grp-1", null, "test-gu-1", false);

        assertThatThrownBy(() ->
                insertGroupMember("test-gm-2", "test-grp-1", null, "test-gu-1", false)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("same guest in different groups is allowed")
    void testSameGuestDifferentGroups() {
        insertTestUser("test-usr-1");
        insertTestGroup("test-grp-1", "test-usr-1");
        insertTestGroup("test-grp-2", "test-usr-1");
        insertTestGuestUser("test-gu-1");

        insertGroupMember("test-gm-1", "test-grp-1", null, "test-gu-1", false);
        insertGroupMember("test-gm-2", "test-grp-2", null, "test-gu-1", false);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_members WHERE id IN ('test-gm-1', 'test-gm-2')",
                Integer.class
        );
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("soft-deleted duplicate is also rejected (V1 full unique constraint uq_group_members_guest still applies)")
    void testDeletedDuplicateAlsoRejected() {
        insertTestUser("test-usr-1");
        insertTestGroup("test-grp-1", "test-usr-1");
        insertTestGuestUser("test-gu-1");

        insertGroupMember("test-gm-1", "test-grp-1", null, "test-gu-1", true);

        assertThatThrownBy(() ->
                insertGroupMember("test-gm-2", "test-grp-1", null, "test-gu-1", false)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("NULL guest_user_id rows are excluded from the unique constraint")
    void testNullGuestUserIdAllowed() {
        insertTestUser("test-usr-1");
        insertTestUser("test-usr-2");
        insertTestGroup("test-grp-1", "test-usr-1");

        insertGroupMember("test-gm-1", "test-grp-1", "test-usr-1", null, false);
        insertGroupMember("test-gm-2", "test-grp-1", "test-usr-2", null, false);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_members WHERE id IN ('test-gm-1', 'test-gm-2')",
                Integer.class
        );
        assertThat(count).isEqualTo(2);
    }

    private void insertTestUser(String id) {
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING",
                id, "clerk-" + id, id + "@test.com", "Test User"
        );
    }

    private void insertTestGroup(String id, String createdBy) {
        jdbcTemplate.update(
                "INSERT INTO groups (id, name, default_currency, created_by) VALUES (?, 'Test Group', 'USD', ?) " +
                "ON CONFLICT (id) DO NOTHING",
                id, createdBy
        );
    }

    private void insertTestGuestUser(String id) {
        jdbcTemplate.update(
                "INSERT INTO guest_users (id, name) VALUES (?, 'Test Guest') " +
                "ON CONFLICT (id) DO NOTHING",
                id
        );
    }

    private void insertGroupMember(String id, String groupId, String userId, String guestUserId, boolean isDeleted) {
        jdbcTemplate.update(
                "INSERT INTO group_members (id, group_id, user_id, guest_user_id, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?)",
                id, groupId, userId, guestUserId, isDeleted
        );
    }
}
