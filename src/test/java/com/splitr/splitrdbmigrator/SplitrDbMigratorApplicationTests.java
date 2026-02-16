package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.liquibase.enabled=false")
@ActiveProfiles("local-postgres")
class SplitrDbMigratorApplicationTests {

    @Test
    void contextLoads() {
    }

}
