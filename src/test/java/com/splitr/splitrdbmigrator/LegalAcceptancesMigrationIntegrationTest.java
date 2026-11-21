package com.splitr.splitrdbmigrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local-postgres")
class LegalAcceptancesMigrationIntegrationTest {

    private static final String SHA = "a".repeat(64);

    private JdbcTemplate jdbcTemplate;
    private String suffix;
    private String userId;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void createUser() {
        suffix = Long.toString(System.nanoTime());
        userId = "usr_legal_" + suffix;
        jdbcTemplate.update(
                "INSERT INTO users (id, clerk_id, email, name, referral_code) VALUES (?, ?, ?, 'Legal', ?)",
                userId, "clerk_legal_" + suffix, "legal-" + suffix + "@example.com", "L" + suffix);
    }

    private int insert(String id, String type, String version, String sha, boolean age, String platform) {
        return jdbcTemplate.update(
                "INSERT INTO legal_acceptances (id, user_id, document_type, document_version, document_sha256, " +
                        "age_confirmed, method, ip_address, user_agent, platform, app_version) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'checkbox_v1', '203.0.113.7', 'jest', ?, '1.1.2')",
                id, userId, type, version, sha, age, platform);
    }

    @Test
    @DisplayName("table has the expected columns and nullability")
    void columnsShape() {
        List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                        "WHERE table_name = 'legal_acceptances' ORDER BY ordinal_position");
        assertThat(cols).extracting(c -> c.get("column_name")).containsExactly(
                "id", "user_id", "document_type", "document_version", "document_sha256", "age_confirmed",
                "method", "ip_address", "user_agent", "platform", "app_version", "accepted_at", "created_at");
        Map<Object, Object> nullable = new java.util.HashMap<>();
        cols.forEach(c -> nullable.put(c.get("column_name"), c.get("is_nullable")));
        assertThat(nullable).containsEntry("user_id", "NO").containsEntry("document_sha256", "NO")
                .containsEntry("accepted_at", "NO").containsEntry("ip_address", "YES")
                .containsEntry("platform", "YES");
        assertThat(cols.stream().filter(c -> c.get("column_name").equals("accepted_at")).findFirst().orElseThrow()
                .get("data_type")).isEqualTo("timestamp with time zone");
    }

    @Test
    @DisplayName("valid TERMS and PRIVACY rows insert with server-side timestamps")
    void validInsert() {
        insert("la_t_" + suffix, "TERMS", "1.0", SHA, true, "ios");
        insert("la_p_" + suffix, "PRIVACY", "1.0", SHA, true, null);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM legal_acceptances WHERE user_id = ? AND accepted_at IS NOT NULL",
                Integer.class, userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("rejects unknown document types")
    void rejectsBadType() {
        assertThatThrownBy(() -> insert("la_bad_" + suffix, "COOKIES", "1.0", SHA, true, "web"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_legal_acceptances_document_type");
    }

    @Test
    @DisplayName("rejects a hash that is not 64 lowercase hex characters")
    void rejectsBadSha() {
        assertThatThrownBy(() -> insert("la_sha_" + suffix, "TERMS", "1.0", "ABC", true, "web"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_legal_acceptances_sha256");
    }

    @Test
    @DisplayName("rejects a row without age confirmation")
    void rejectsNoAge() {
        assertThatThrownBy(() -> insert("la_age_" + suffix, "TERMS", "1.0", SHA, false, "web"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_legal_acceptances_age");
    }

    @Test
    @DisplayName("rejects unknown platforms")
    void rejectsBadPlatform() {
        assertThatThrownBy(() -> insert("la_plat_" + suffix, "TERMS", "1.0", SHA, true, "desktop"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_legal_acceptances_platform");
    }

    @Test
    @DisplayName("one row per user, document and version; a new version is a new row")
    void uniquePerVersion() {
        insert("la_u1_" + suffix, "TERMS", "1.0", SHA, true, "web");
        insert("la_u2_" + suffix, "TERMS", "1.1", SHA, true, "web");
        assertThatThrownBy(() -> insert("la_u3_" + suffix, "TERMS", "1.0", SHA, true, "web"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_legal_acceptances_user_doc_version");
    }

    @Test
    @DisplayName("user_id must reference an existing user")
    void foreignKey() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO legal_acceptances (id, user_id, document_type, document_version, document_sha256, " +
                        "age_confirmed, method) VALUES (?, 'usr_missing', 'TERMS', '1.0', ?, TRUE, 'checkbox_v1')",
                "la_fk_" + suffix, SHA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("lookup index exists")
    void indexExists() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'legal_acceptances'", String.class))
                .contains("idx_legal_acceptances_user_doc_time");
    }
}
