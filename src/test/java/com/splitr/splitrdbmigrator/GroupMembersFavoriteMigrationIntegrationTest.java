package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class GroupMembersFavoriteMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private Map<String, Object> column(String name) {
        return jdbcTemplate.queryForMap(
                "SELECT data_type, is_nullable, column_default FROM information_schema.columns " +
                        "WHERE table_name = 'group_members' AND column_name = ?",
                name
        );
    }

    @Test
    @DisplayName("is_favorite is a NOT NULL boolean that defaults to false")
    void isFavoriteColumnShape() {
        Map<String, Object> col = column("is_favorite");
        assertThat(col.get("data_type")).isEqualTo("boolean");
        assertThat(col.get("is_nullable")).isEqualTo("NO");
        assertThat(col.get("column_default")).isEqualTo("false");
    }

    @Test
    @DisplayName("favorited_at is a nullable timestamptz")
    void favoritedAtColumnShape() {
        Map<String, Object> col = column("favorited_at");
        assertThat(col.get("data_type")).isEqualTo("timestamp with time zone");
        assertThat(col.get("is_nullable")).isEqualTo("YES");
    }

    @Test
    @DisplayName("check constraint keeps is_favorite and favorited_at in agreement")
    void favoritedAtCheckConstraint() {
        String suffix = Long.toString(System.nanoTime());
        String userId = "usr_fav_" + suffix;
        String groupId = "grp_fav_" + suffix;
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name, referral_code) VALUES (?, ?, ?, 'Fav', ?)",
                userId, "clerk_fav_" + suffix, "fav-" + suffix + "@example.com", "F" + suffix);
        jdbcTemplate.update(
                "INSERT INTO groups (id, name, created_by, invite_code) VALUES (?, 'Fav group', ?, ?)",
                groupId, userId, "I" + suffix.substring(suffix.length() - 7));

        jdbcTemplate.update(
                "INSERT INTO group_members (id, group_id, user_id, is_favorite, favorited_at) VALUES (?, ?, ?, TRUE, NOW())",
                "gm_ok_" + suffix, groupId, userId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_favorite FROM group_members WHERE id = ?", Boolean.class, "gm_ok_" + suffix)).isTrue();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE group_members SET favorited_at = NULL WHERE id = ?", "gm_ok_" + suffix))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_group_members_favorited_at");
    }
}
