package com.hyeja.global.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeRangeMigrationTest {

    @Test
    void convertsLegacyIncomeRangesIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:income-range-migration;MODE=MariaDB")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE profile (email VARCHAR(100), income_range_code VARCHAR(20))");
                statement.execute("""
                        INSERT INTO profile VALUES
                            ('under@test', 'INC_0_20'),
                            ('range2@test', 'INC_20_30'),
                            ('range3@test', 'INC_30_40'),
                            ('range4@test', 'INC_40_UP'),
                            ('current@test', 'OVER_5000'),
                            ('empty@test', NULL)
                        """);
            }

            ClassPathResource migration = new ClassPathResource(
                    "db/migration/normalize-income-range.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT email, income_range_code FROM profile")) {
                Map<String, String> expected = Map.of(
                        "under@test", "UNDER_2000",
                        "range2@test", "R2000_3000",
                        "range3@test", "R3000_4000",
                        "range4@test", "R4000_5000",
                        "current@test", "OVER_5000");
                int rowCount = 0;
                while (result.next()) {
                    String email = result.getString("email");
                    assertThat(result.getString("income_range_code")).isEqualTo(expected.get(email));
                    rowCount++;
                }
                assertThat(rowCount).isEqualTo(6);
            }
        }
    }
}
