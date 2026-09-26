package com.hyeja.e2e.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class E2eContainerConfiguration {

    @Bean
    @ServiceConnection
    MariaDBContainer mariaDbContainer() {
        return new MariaDBContainer(DockerImageName.parse("mariadb:11.4.8"))
                .withDatabaseName("hyeja_e2e")
                .withUsername("hyeja")
                .withPassword("hyeja");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.2.10-alpine"))
                .withExposedPorts(6379);
    }

    @Bean
    @Primary
    Clock e2eClock() {
        return Clock.fixed(
                Instant.parse("2026-09-27T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
    }
}
