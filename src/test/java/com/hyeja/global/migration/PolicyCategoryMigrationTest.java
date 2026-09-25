package com.hyeja.global.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class PolicyCategoryMigrationTest {

    @Test
    void convertsLegacyHousingCategoryToOtherIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:policy-category-migration;MODE=MariaDB")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE policy (policy_id VARCHAR(30), category VARCHAR(20))");
                statement.execute("INSERT INTO policy VALUES ('legacy', '주거'), "
                        + "('current', 'MONTHLY_RENT')");
            }

            ClassPathResource migration = new ClassPathResource(
                    "db/migration/normalize-policy-category.sql");
            EncodedResource utf8Migration = new EncodedResource(migration, StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(connection, utf8Migration);
            ScriptUtils.executeSqlScript(connection, utf8Migration);

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT policy_id, category FROM policy ORDER BY policy_id")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("policy_id")).isEqualTo("current");
                assertThat(result.getString("category")).isEqualTo("MONTHLY_RENT");
                assertThat(result.next()).isTrue();
                assertThat(result.getString("policy_id")).isEqualTo("legacy");
                assertThat(result.getString("category")).isEqualTo("OTHER");
                assertThat(result.next()).isFalse();
            }
        }
    }
}
