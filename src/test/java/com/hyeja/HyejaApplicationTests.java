package com.hyeja;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HyejaApplicationTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void healthCheckReturnsOkWithoutAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        var body = JsonPath.parse(response.body());
        assertThat(body.read("$.isSuccess", Boolean.class)).isTrue();
        assertThat(body.read("$.code", String.class)).isEqualTo("SUCCESS_001");
        assertThat(body.read("$.result.status", String.class)).isEqualTo("UP");
    }

    @Test
    void openApiDocumentContainsHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        var document = JsonPath.parse(response.body());
        assertThat(document.read("$.info.title", String.class)).isEqualTo("혜자 API");
        assertThat(document.read("$.paths['/api/health'].get.summary", String.class))
                .isEqualTo("헬스체크");
        Map<?, ?> paths = document.read("$.paths");
        assertThat(paths.containsKey("/api/health")).isTrue();
        assertThat(paths.containsKey("/error")).isFalse();
    }

    @Test
    void swaggerUiIsAccessible() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/swagger-ui.html"))
                .GET().build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("text/html");
        assertThat(response.body()).contains("swagger-ui-bundle.js");
    }

    // 회원은 토큰으로 식별하므로 정책 상세 문서에는 policyId만 노출됩니다(memberId는 숨김).
    @Test
    void policyDetailOpenApiExposesOnlyPolicyId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> parameterNames = JsonPath.parse(response.body()).read(
                "$.paths['/api/policies/{policyId}'].get.parameters[*].name");
        assertThat(parameterNames).containsExactly("policyId");
    }
}
