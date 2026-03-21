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
class ProfileImageClearedMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testProfileImageClearedColumnExistsAndIsBoolean() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'profile_image_cleared'",
                String.class
        );
        assertThat(dataType)
                .as("Column users.profile_image_cleared should be boolean")
                .isEqualTo("boolean");
    }

    @Test
    void testProfileImageClearedIsNotNullable() {
        String isNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'profile_image_cleared'",
                String.class
        );
        assertThat(isNullable)
                .as("Column users.profile_image_cleared should be NOT NULL")
                .isEqualTo("NO");
    }

    @Test
    void testProfileImageClearedDefaultValue() {
        String columnDefault = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'profile_image_cleared'",
                String.class
        );
        assertThat(columnDefault)
                .as("Column users.profile_image_cleared should default to false")
                .isEqualTo("false");
    }

    @Test
    void testProfileImageClearedComment() {
        String comment = jdbcTemplate.queryForObject(
                "SELECT pg_catalog.col_description(c.oid, cols.ordinal_position::int) " +
                        "FROM pg_catalog.pg_class c " +
                        "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
                        "JOIN information_schema.columns cols ON cols.table_name = c.relname " +
                        "WHERE c.relname = 'users' AND cols.column_name = 'profile_image_cleared' AND n.nspname = 'public'",
                String.class
        );
        assertThat(comment).isEqualTo("When true, Clerk avatar sync is suppressed because user explicitly deleted their profile image");
    }
}
