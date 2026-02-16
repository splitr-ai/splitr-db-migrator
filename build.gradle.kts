plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.splitr"
version = "0.0.1-SNAPSHOT"
description = "splitr-db-migrator"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)

    implementation(libs.postgresql)
    implementation(libs.aws.advanced.jdbc.wrapper)

    // Lombok for annotations like @Slf4j
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.liquibase.test)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}


fun registerMigrationTask(
    name: String,
    dataSourceProfile: String,
    dbName: String = "splitr-db",
    repave: Boolean = false,
    desc: String,
    liquibaseLabels: String = "DDL,DML",
    nextTask: Task? = null
) {
    tasks.register(name, JavaExec::class) {
        group = "Application"

        description = desc
        classpath = sourceSets.main.get().runtimeClasspath

        mainClass.set("com.splitr.splitrdbmigrator.SplitrDbMigratorApplication")

        environment("REPAVE_DB", repave)
        args = listOf(
            "--spring.profiles.active=$dataSourceProfile"
        )

        nextTask?.let { dependsOn(it) }
    }
}

registerMigrationTask(
    "migrate",
    repave = false,
    dataSourceProfile = "local-postgres",
    desc = "Migrates the database schema and data for local development",
    liquibaseLabels = "DDL,DML",
    dbName = "splitr-db"
)

// Alias task to run Liquibase updates only (no repave)
registerMigrationTask(
    "updateDb",
    repave = false,
    dataSourceProfile = "local-postgres",
    desc = "Runs Liquibase updates only (no repave) for local development",
    liquibaseLabels = "DDL,DML",
    dbName = "splitr-db"
)

registerMigrationTask(
    "repaveDb",
    repave = true,
    dataSourceProfile = "local-postgres-repave",
    desc = "Deletes and recreates the database schema and data for local development",
    liquibaseLabels = "DDL,DML",
    dbName = "splitr-db"
)

// Make repaveDb task finalize with migrate task so migrate runs after repaveDb
tasks.getByName("repaveDb").finalizedBy(tasks.getByName("migrate"))



tasks.withType<Test> {
    useJUnitPlatform()
}
