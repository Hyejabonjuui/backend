package com.hyeja.global.seed;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.profile.entity.Profile;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hyeja-seed;MODE=MariaDB;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=always"
})
@Transactional
class SeedDataTest {

    private static final List<String> TABLES = List.of(
            "member", "profile", "region", "policy", "policy_region",
            "card_news", "favorite", "notification", "term");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    void startupCreatesTenRowsPerTableWithTimestamps() {
        for (String table : TABLES) {
            assertThat(count(table)).as(table).isEqualTo(10);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table
                    + " WHERE created_at IS NULL OR updated_at IS NULL OR deleted_at IS NOT NULL", Long.class))
                    .as(table + " timestamps").isZero();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE read_yn = TRUE", Long.class))
                .isEqualTo(5);
    }

    @Test
    void seededEntitiesLoadWithRelationshipsAndExpandedColumns() {
        Profile profile = entityManager.find(Profile.class, "seed01@hyeja.test");
        assertThat(profile.getMember().getEmail()).isEqualTo("seed01@hyeja.test");
        assertThat(profile.getRegion().getRegionCode()).isEqualTo("11440");
        assertThat(profile.getHousingType()).isEqualTo("MONTHLY_RENT");
        Policy policy = entityManager.find(Policy.class, "DEMO-HOUSING-001");
        assertThat(policy.getHousingType()).isEqualTo("MONTHLY_RENT");
        CardNews card = entityManager.createQuery(
                "select c from CardNews c where c.policy.policyId = :id", CardNews.class)
                .setParameter("id", policy.getPolicyId()).getSingleResult();
        assertThat(card.getTitle()).isEqualTo("[시연] 월세 부담 완화");
        assertThat(card.getCardNo()).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM favorite f
                JOIN member m ON m.member_id = f.member_id
                JOIN profile pr ON pr.email = m.email
                JOIN policy_region r ON r.policy_id = f.policy_id AND r.region_code = pr.region_code
                JOIN card_news c ON c.policy_id = f.policy_id
                JOIN notification n ON n.member_id = m.member_id AND n.policy_id = f.policy_id
                """, Long.class)).isEqualTo(10);
    }

    @Test
    void allTestAccountsHaveValidBcryptPasswords() {
        List<String> passwords = jdbc.queryForList("SELECT password FROM member", String.class);
        assertThat(passwords).hasSize(10).allSatisfy(password -> {
            assertThat(password).isNotEqualTo("Hyeja1234!");
            assertThat(BCrypt.checkpw("Hyeja1234!", password)).isTrue();
            assertThat(BCrypt.checkpw("wrong-password", password)).isFalse();
        });
    }

    @Test
    void rerunDoesNotDuplicateOverwriteOrRestoreExistingRows() {
        jdbc.update("UPDATE member SET nickname = '수정한 닉네임' WHERE email = 'seed01@hyeja.test'");
        jdbc.update("UPDATE profile SET housing_type = 'JEONSE' WHERE email = 'seed01@hyeja.test'");
        jdbc.update("UPDATE region SET sigungu_name = '수정한 지역명' WHERE region_code = '11440'");
        jdbc.update("UPDATE policy SET view_count = 123, active_yn = FALSE WHERE policy_id = 'DEMO-HOUSING-001'");
        jdbc.update("UPDATE policy_region SET deleted_at = CURRENT_TIMESTAMP WHERE policy_id = 'DEMO-HOUSING-001'");
        jdbc.update("UPDATE card_news SET title = '수정한 제목' WHERE policy_id = 'DEMO-HOUSING-001'");
        jdbc.update("UPDATE favorite SET deleted_at = CURRENT_TIMESTAMP WHERE policy_id = 'DEMO-HOUSING-001'");
        jdbc.update("UPDATE notification SET read_yn = TRUE WHERE policy_id = 'DEMO-HOUSING-001'");
        jdbc.update("UPDATE term SET easy_description = '수정한 설명' WHERE term = '무주택자'");
        jdbc.update("""
                INSERT INTO member (email, password, nickname, role, created_at, updated_at)
                VALUES ('existing@hyeja.test', 'existing-hash', '기존 회원', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        var before = snapshot();

        executeSeed();
        executeSeed();

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void seedsNonEmptyDatabaseWithoutDependingOnGeneratedIds() {
        for (String table : List.of("notification", "favorite", "card_news", "policy_region",
                "profile", "policy", "member", "region", "term")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("""
                INSERT INTO member (member_id, email, password, nickname, role, created_at, updated_at)
                VALUES (1, 'existing@hyeja.test', 'existing-hash', '기존 회원', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO region (region_code, sigungu_name, created_at, updated_at)
                VALUES ('11440', '기존 지역명', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO term (term, easy_description, created_at, updated_at)
                VALUES ('무주택자', '기존 설명', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        executeSeed();

        for (String table : TABLES) {
            assertThat(count(table)).as(table).isEqualTo(table.equals("member") ? 11 : 10);
        }
        assertThat(jdbc.queryForObject("SELECT email FROM member WHERE member_id = 1", String.class))
                .isEqualTo("existing@hyeja.test");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM favorite WHERE member_id = 1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE member_id = 1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT sigungu_name FROM region WHERE region_code = '11440'", String.class))
                .isEqualTo("기존 지역명");
        assertThat(jdbc.queryForObject("SELECT easy_description FROM term WHERE term = '무주택자'", String.class))
                .isEqualTo("기존 설명");
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : TABLES) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1, 2"));
        }
        return result;
    }

    private void executeSeed() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/dev-data.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}
