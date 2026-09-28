package com.hyeja.global.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ParentCityRegionMigrationTest {

    // 상위 시(41110 수원시)는 지우고 구(41111)는 남깁니다. 회원 조건이 참조 중인 상위 시(41130 성남시)는 지우지 않습니다.
    @Test
    void removesUnreferencedParentCitiesIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:parent-city-migration;MODE=MariaDB")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE region (region_code VARCHAR(5) PRIMARY KEY, sigungu_name VARCHAR(50))");
                statement.execute("CREATE TABLE profile (email VARCHAR(100), region_code VARCHAR(5))");
                statement.execute("CREATE TABLE policy_region (policy_id VARCHAR(30), region_code VARCHAR(5))");
                statement.execute("""
                        INSERT INTO region VALUES
                            ('41110', '경기도 수원시'),
                            ('41111', '경기도 수원시 장안구'),
                            ('41130', '경기도 성남시'),
                            ('11440', '서울특별시 마포구')
                        """);
                statement.execute("INSERT INTO profile VALUES ('member@example.com', '41130')");
            }

            ClassPathResource migration = new ClassPathResource("db/migration/remove-parent-city-regions.sql");
            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);

            List<String> codes = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("SELECT region_code FROM region ORDER BY region_code")) {
                while (result.next()) {
                    codes.add(result.getString(1));
                }
            }
            assertThat(codes).containsExactly("11440", "41111", "41130");
        }
    }
}
