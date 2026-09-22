package com.hyeja.global.seed;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hyeja-no-seed;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SeedDisabledTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void tablesRemainEmptyWithTestProfile() {
        for (String table : List.of("member", "profile", "region", "policy", "policy_region",
                "card_news", "favorite", "notification", "term")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class))
                    .as(table).isZero();
        }
    }
}
