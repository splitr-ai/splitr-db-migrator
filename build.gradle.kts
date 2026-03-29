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


// Load environment-specific .env file as a map of key=value pairs
// Usage: .env.dev for dev, .env.prod for prod
fun loadEnvFile(envName: String): Map<String, String> {
    val envFile = rootProject.file(".env.$envName")
    if (!envFile.exists()) {
        throw GradleException(".env.$envName not found. Create it with RAILWAY_DB_* variables.")
    }
    return envFile.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val (key, value) = line.split("=", limit = 2)
            key.trim() to value.trim()
        }
}

fun registerMigrationTask(
    name: String,
    dataSourceProfile: String,
    dbName: String = "splitr-db",
    repave: Boolean = false,
    desc: String,
    liquibaseLabels: String = "DDL,DML",
    envFile: String? = null,
    nextTask: Task? = null
) {
    tasks.register(name, JavaExec::class) {
        group = "Application"

        description = desc
        classpath = sourceSets.main.get().runtimeClasspath

        mainClass.set("com.splitr.splitrdbmigrator.SplitrDbMigratorApplication")

        environment("REPAVE_DB", repave)
        if (envFile != null) {
            environment(loadEnvFile(envFile))
        }
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

registerMigrationTask(
    "cleanDb",
    repave = false,
    dataSourceProfile = "local-postgres-clean",
    desc = "Cleans the database schema and data by dropping all objects and re-running migrations",
    liquibaseLabels = "DDL,DML",
    dbName = "splitr-db"
)

// Railway migrations — each environment loads its own .env.{env} file.
// Usage: ./gradlew migrateDev   (reads .env.dev)
//        ./gradlew migrateProd  (reads .env.prod)
registerMigrationTask(
    "migrateDev",
    repave = false,
    dataSourceProfile = "railway-dev",
    desc = "Runs Liquibase migrations against Railway dev Postgres (via TCP proxy)",
    liquibaseLabels = "DDL,DML",
    envFile = "dev",
    dbName = "railway"
)

registerMigrationTask(
    "migrateProd",
    repave = false,
    dataSourceProfile = "railway-prod",
    desc = "Runs Liquibase migrations against Railway prod Postgres (via TCP proxy)",
    liquibaseLabels = "DDL,DML",
    envFile = "prod",
    dbName = "railway"
)

// Make repaveDb task finalize with migrate task so migrate runs after repaveDb
tasks.getByName("repaveDb").finalizedBy(tasks.getByName("migrate"))



tasks.withType<Test> {
    useJUnitPlatform()
}
