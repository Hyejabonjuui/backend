package com.hyeja.global.seed;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
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

    private static final List<String> NON_TERM_TABLES = List.of(
            "member", "profile", "region", "policy", "policy_region",
            "card_news", "favorite", "notification");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void startupCreatesOnlyTerms() {
        assertThat(count("term")).isEqualTo(50);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM term
                WHERE created_at IS NULL OR updated_at IS NULL OR deleted_at IS NOT NULL
                """, Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM term WHERE term = '중위소득'", Long.class))
                .isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT term) FROM term", Long.class))
                .isEqualTo(50);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM term WHERE example LIKE '시연 예시:%'", Long.class))
                .isZero();

        for (String table : NON_TERM_TABLES) {
            assertThat(count(table)).as(table).isZero();
        }
    }

    @Test
    void rerunDoesNotDuplicateOrOverwriteModifiedTerm() {
        jdbc.update("UPDATE term SET easy_description = '수정한 설명' WHERE term = '무주택자'");
        var before = jdbc.queryForList("SELECT * FROM term ORDER BY term");

        executeSeed();
        executeSeed();

        assertThat(jdbc.queryForList("SELECT * FROM term ORDER BY term")).isEqualTo(before);
    }

    @Test
    void rerunRefinesUnmodifiedLegacyTerm() {
        jdbc.update("""
                UPDATE term
                SET easy_description = '우리나라 모든 가구를 소득 순서로 세웠을 때 가운데 가구의 소득입니다.',
                    example = '시연 예시: 중위소득 60% 이하'
                WHERE term = '중위소득'
                """);

        executeSeed();

        assertThat(jdbc.queryForObject(
                "SELECT easy_description FROM term WHERE term = '중위소득'", String.class))
                .isEqualTo("정부가 가구 소득을 비교하기 위해 정하는 가운데 기준값입니다. "
                        + "정책에서는 가구원 수별 금액을 사용합니다.");
        assertThat(jdbc.queryForObject(
                "SELECT example FROM term WHERE term = '중위소득'", String.class))
                .isEqualTo("기준 중위소득 60% 이하");
    }

    @Test
    void seedLeavesUnrelatedDataUntouched() {
        jdbc.update("DELETE FROM term");
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

        assertThat(count("term")).isEqualTo(50);
        assertThat(count("member")).isOne();
        assertThat(count("region")).isOne();
        for (String table : List.of(
                "profile", "policy", "policy_region", "card_news", "favorite", "notification")) {
            assertThat(count(table)).as(table).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT email FROM member WHERE member_id = 1", String.class))
                .isEqualTo("existing@hyeja.test");
        assertThat(jdbc.queryForObject(
                "SELECT sigungu_name FROM region WHERE region_code = '11440'", String.class))
                .isEqualTo("기존 지역명");
        assertThat(jdbc.queryForObject(
                "SELECT easy_description FROM term WHERE term = '무주택자'", String.class))
                .isEqualTo("기존 설명");
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private void executeSeed() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/dev-data.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}
