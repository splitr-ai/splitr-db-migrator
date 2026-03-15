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
class SimplifyDebtsDefaultIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testSimplifyDebtsDefaultValue() {
        String columnDefault = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns WHERE table_name = 'groups' AND column_name = 'simplify_debts'",
                String.class
        );
        
        assertThat(columnDefault).isEqualTo("false");
    }
}
