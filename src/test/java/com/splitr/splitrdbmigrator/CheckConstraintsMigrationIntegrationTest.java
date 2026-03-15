package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class CheckConstraintsMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testCheckConstraintsExist() {
        List<String> expectedConstraints = List.of(
                "chk_member_identity",
                "chk_payer_identity",
                "chk_split_identity"
        );

        for (String constraintName : expectedConstraints) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.constraint_column_usage WHERE constraint_name = ?",
                    Integer.class,
                    constraintName
            );
            assertThat(count).as("Constraint %s should exist", constraintName).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void testGroupMembersCheckConstraint() {
        // Both null should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO group_members (id, group_id, user_id, guest_user_id) VALUES ('test-gm-1', '1', NULL, NULL)");
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_member_identity");
    }

    @Test
    void testExpensePayersCheckConstraint() {
        // Both null should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO expense_payers (id, expense_id, user_id, guest_user_id, amount_paid) VALUES ('test-ep-1', '1', NULL, NULL, 10)");
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_payer_identity");
    }

    @Test
    void testExpenseSplitsCheckConstraint() {
        // Both null should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO expense_splits (id, expense_id, user_id, guest_user_id, split_amount) VALUES ('test-es-1', '1', NULL, NULL, 10)");
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_split_identity");
    }
}
