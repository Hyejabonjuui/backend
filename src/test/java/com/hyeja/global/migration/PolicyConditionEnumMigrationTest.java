package com.hyeja.global.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class PolicyConditionEnumMigrationTest {

    @Test
    void normalizesEveryLegacyApplyPeriodIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:policy-condition-migration;MODE=MariaDB")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE policy (
                            policy_id VARCHAR(30),
                            houseless_yn VARCHAR(20),
                            apply_period_code VARCHAR(20) NOT NULL
                        )
                        """);
                statement.execute("""
                        INSERT INTO policy VALUES
                            ('period', 'UNKNOWN', 'PERIOD'),
                            ('unknown', 'UNKNOWN', 'UNKNOWN'),
                            ('unexpected', 'UNKNOWN', 'UNEXPECTED'),
                            ('specific-code', 'UNKNOWN', '0057001'),
                            ('always-code', 'UNKNOWN', '57002'),
                            ('closed-code', 'UNKNOWN', '0057003'),
                            ('current-specific', 'UNKNOWN', 'SPECIFIC_PERIOD'),
                            ('current-always', 'UNKNOWN', 'ALWAYS'),
                            ('current-closed', 'UNKNOWN', 'CLOSED')
                        """);
            }

            ClassPathResource migration = new ClassPathResource(
                    "db/migration/normalize-policy-condition-enums.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);

            Map<String, String> expected = Map.of(
                    "period", "SPECIFIC_PERIOD",
                    "unknown", "SPECIFIC_PERIOD",
                    "unexpected", "SPECIFIC_PERIOD",
                    "specific-code", "SPECIFIC_PERIOD",
                    "always-code", "ALWAYS",
                    "closed-code", "CLOSED",
                    "current-specific", "SPECIFIC_PERIOD",
                    "current-always", "ALWAYS",
                    "current-closed", "CLOSED");
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT policy_id, apply_period_code FROM policy")) {
                int rowCount = 0;
                while (result.next()) {
                    assertThat(result.getString("apply_period_code"))
                            .isEqualTo(expected.get(result.getString("policy_id")));
                    rowCount++;
                }
                assertThat(rowCount).isEqualTo(expected.size());
            }
        }
    }
}
