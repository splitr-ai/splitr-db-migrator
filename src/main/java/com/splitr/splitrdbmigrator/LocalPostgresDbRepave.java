package com.splitr.splitrdbmigrator;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Slf4j
@Component
@Profile({"local-postgres-repave"})
@ConditionalOnProperty(prefix = "postgres", name = "repave", havingValue = "true")
public class LocalPostgresDbRepave {

    @Value("${postgres.db.name:splitr-db}")
    private String dbName;
    @Value("${postgres.repave:true}")
    private boolean repave;
    private final DataSource systemDataSource;

    public LocalPostgresDbRepave(DataSource systemDataSource) {
        this.systemDataSource = systemDataSource;
    }

    @PostConstruct
    public void repaveSplitrDb() {
        log.info("Repaving the database...");
        if (repave) {
            var template = jdbcTemplate();
            var sql = "select count(*) from pg_database where datname = ?";
            var exists = template.queryForObject(sql, Integer.class, dbName);
            if (exists != null && exists != 0) {
                var dropSql = "DROP DATABASE IF EXISTS \"" + dbName + "\"";
                log.info("Database {} exists, dropping it...", dbName);
                template.execute(dropSql);
            }

            String createDbSql = "CREATE DATABASE \"" + dbName + "\"";
            log.info("Creating database {}...", dbName);
            template.execute(createDbSql);
        } else {
            log.info("Database repaving is disabled. Skipping repave operation.");
        }
    }

    JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(systemDataSource);
    }
}
