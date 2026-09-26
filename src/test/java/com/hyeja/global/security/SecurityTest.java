package com.hyeja.global.security;

import com.hyeja.domain.member.entity.Member;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 실제 서버를 띄워 Security 설정·인증 필터가 함께 동작하는지 확인합니다.
// Redis는 테스트 환경에 없으므로 블랙리스트만 가짜(mock)로 바꿉니다.
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    @Test
    void logoutWithValidTokenBlacklistsIt() throws Exception {
        String token = jwtProvider.createAccessToken(member(1L));

        HttpResponse<String> response = logout("Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.parse(response.body()).read("$.code", String.class)).isEqualTo("SUCCESS_001");
        verify(tokenBlacklist).add(eq(token), any(Duration.class));
    }

    // 토큰 없음·위조 토큰·이미 로그아웃한 토큰은 모두 공통 응답 형식의 401입니다.
    @Test
    void logoutWithoutValidTokenIsUnauthorized() throws Exception {
        String loggedOut = jwtProvider.createAccessToken(member(2L));
        when(tokenBlacklist.contains(loggedOut)).thenReturn(true);

        for (String header : new String[] {null, "Bearer not-a-jwt", "Bearer " + loggedOut}) {
            HttpResponse<String> response = logout(header);

            assertThat(response.statusCode()).isEqualTo(401);
            var body = JsonPath.parse(response.body());
            assertThat(body.read("$.isSuccess", Boolean.class)).isFalse();
            assertThat(body.read("$.code", String.class)).isEqualTo("COMMON_002");
            assertThat(body.read("$.message", String.class)).isEqualTo("인증이 필요합니다.");
        }
    }

    // memberId 쿼리 파라미터 → 토큰 전환 전까지 다른 API는 토큰 없이도 호출됩니다.
    @Test
    void otherApisStillWorkWithoutToken() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> logout(String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/members/logout"))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Member member(Long memberId) {
        Member member = Member.builder().email("hyeja@example.com").password("encoded").nickname("민지").build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }
}
