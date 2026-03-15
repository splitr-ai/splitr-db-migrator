package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class GuestUserEmailUniqueIndexIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_guest_users_email_unique'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testMultipleNullEmailsAreAllowed() {
        // Cleanup potentially conflicting guests from other tests
        jdbcTemplate.execute("DELETE FROM guest_users WHERE email IS NULL");

        // Insert first guest with null email
        jdbcTemplate.execute("INSERT INTO guest_users (id, name, email) VALUES ('guest-null-1', 'Null 1', NULL)");
        // Insert second guest with null email
        jdbcTemplate.execute("INSERT INTO guest_users (id, name, email) VALUES ('guest-null-2', 'Null 2', NULL)");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM guest_users WHERE email IS NULL",
                Integer.class
        );
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testDuplicateEmailsAreForbidden() {
        String testEmail = "duplicate@example.com";
        // Cleanup
        jdbcTemplate.execute("DELETE FROM guest_users WHERE email = '" + testEmail + "'");

        // Insert first guest
        jdbcTemplate.execute("INSERT INTO guest_users (id, name, email) VALUES ('guest-dup-1', 'Dup 1', '" + testEmail + "')");

        // Insert second guest with same email should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO guest_users (id, name, email) VALUES ('guest-dup-2', 'Dup 2', '" + testEmail + "')");
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
