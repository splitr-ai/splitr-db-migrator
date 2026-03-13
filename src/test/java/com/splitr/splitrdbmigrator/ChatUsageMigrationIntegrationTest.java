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
class ChatUsageMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testChatUsageTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'chat_usage'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testChatUsageIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'idx_chat_usage_user_used_at'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testChatUsageColumnsExist() {
        Integer promptTokensCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'chat_usage' AND column_name = 'prompt_tokens'",
                Integer.class
        );
        assertThat(promptTokensCount).isEqualTo(1);

        Integer completionTokensCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'chat_usage' AND column_name = 'completion_tokens'",
                Integer.class
        );
        assertThat(completionTokensCount).isEqualTo(1);

        Integer modelNameCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'chat_usage' AND column_name = 'model_name'",
                Integer.class
        );
        assertThat(modelNameCount).isEqualTo(1);
    }
}
