package com.splitr.splitrdbmigrator;

import liquibase.GlobalConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;

@SpringBootConfiguration
@ImportAutoConfiguration(
        classes = {
                DataSourceAutoConfiguration.class,
                LiquibaseAutoConfiguration.class,
                // Local-only helpers (guarded by @Profile)
                LocalPostgresDbRepave.class,
                LocalDataSourceConfiguration.class,
                GlobalConfiguration.class
        }
)
public class SplitrDbMigratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitrDbMigratorApplication.class, args);
    }

}
