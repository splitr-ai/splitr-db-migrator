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
class ImageKeysMigrationIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testProfileImageKeyColumnExists() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'profile_image_key'",
                String.class
        );
        assertThat(dataType).isEqualTo("character varying");
    }

    @Test
    void testBannerImageKeyColumnExists() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'groups' AND column_name = 'banner_image_key'",
                String.class
        );
        assertThat(dataType).isEqualTo("character varying");
    }

    @Test
    void testReceiptImageKeyColumnExists() {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns WHERE table_name = 'expenses' AND column_name = 'receipt_image_key'",
                String.class
        );
        assertThat(dataType).isEqualTo("character varying");
    }
}
