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
class ExpenseCommentsMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testExpenseCommentsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'expense_comments'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testExpenseCommentsColumnsExist() {
        List<String> expectedColumns = List.of(
                "id", "expense_id", "comment_text", "author_user_id", "author_guest_id",
                "created_at", "updated_at", "is_deleted", "version"
        );

        for (String column : expectedColumns) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.columns WHERE table_name = 'expense_comments' AND column_name = ?",
                    Integer.class,
                    column
            );
            assertThat(count).as("Column %s should exist", column).isEqualTo(1);
        }
    }

    @Test
    void testExpenseCommentsIndexesExist() {
        List<String> expectedIndexes = List.of(
                "idx_expense_comments_expense_id",
                "idx_expense_comments_author_user_id",
                "idx_expense_comments_author_guest_id"
        );

        for (String indexName : expectedIndexes) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_indexes WHERE indexname = ?",
                    Integer.class,
                    indexName
            );
            assertThat(count).as("Index %s should exist", indexName).isEqualTo(1);
        }
    }

    @Test
    void testCommentAuthorCheckConstraint() {
        // Both null should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO expense_comments (id, expense_id, comment_text, author_user_id, author_guest_id) " +
                    "VALUES ('test-ec-1', 'exp-1', 'hello', NULL, NULL)");
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_comment_author");

        // Both NOT null should fail
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("INSERT INTO expense_comments (id, expense_id, comment_text, author_user_id, author_guest_id) " +
                    "VALUES ('test-ec-2', 'exp-1', 'hello', 'user-1', 'guest-1')");
        }).isInstanceOf(DataIntegrityViolationException.class)
          .hasMessageContaining("chk_comment_author");
    }
}
