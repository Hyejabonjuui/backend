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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
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

        HttpResponse<String> response = send("POST", "/api/members/logout", "Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.parse(response.body()).read("$.code", String.class)).isEqualTo("SUCCESS_001");
        verify(tokenBlacklist).add(eq(token), any(Duration.class));
    }

    // 토큰 없음·위조 토큰·이미 로그아웃한 토큰은 모두 공통 응답 형식의 401입니다.
    @Test
    void rejectsMissingInvalidOrLoggedOutToken() throws Exception {
        String loggedOut = jwtProvider.createAccessToken(member(2L));
        when(tokenBlacklist.contains(loggedOut)).thenReturn(true);

        for (String header : new String[] {null, "Bearer not-a-jwt", "Bearer " + loggedOut}) {
            HttpResponse<String> response = send("POST", "/api/members/logout", header);

            assertThat(response.statusCode()).isEqualTo(401);
            var body = JsonPath.parse(response.body());
            assertThat(body.read("$.isSuccess", Boolean.class)).isFalse();
            assertThat(body.read("$.code", String.class)).isEqualTo("COMMON_002");
            assertThat(body.read("$.message", String.class)).isEqualTo("인증이 필요합니다.");
        }
    }

    // 회원 본인의 정보를 다루는 API는 토큰이 없으면 컨트롤러까지 가지 않고 401입니다.
    @Test
    void memberApisRequireToken() throws Exception {
        String[][] apis = {
                {"GET", "/api/members/me"},
                {"PATCH", "/api/members/me/delete"},
                {"GET", "/api/members/me/profile"},
                {"PATCH", "/api/members/me/profile"},
                {"GET", "/api/favorite"},
                {"POST", "/api/favorite/policy-1"},
                {"DELETE", "/api/favorite/policy-1"},
                {"GET", "/api/notification"},
                {"PATCH", "/api/notification/1/read"},
                {"DELETE", "/api/notification/1"},
                {"GET", "/api/policies/policy-1"},
        };
        for (String[] api : apis) {
            HttpResponse<String> response = send(api[0], api[1], null);

            assertThat(response.statusCode()).as(api[0] + " " + api[1]).isEqualTo(401);
            assertThat(response.body()).as(api[0] + " " + api[1]).contains("COMMON_002");
        }
    }

    // 토큰의 회원 ID가 컨트롤러의 memberId로 전달됩니다. (DB에 없는 회원이라 서비스가 MEMBER_001을 냅니다)
    @Test
    void passesMemberIdFromTokenToController() throws Exception {
        String token = jwtProvider.createAccessToken(member(99L));

        HttpResponse<String> response = send("GET", "/api/members/me", "Bearer " + token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.parse(response.body()).read("$.code", String.class)).isEqualTo("MEMBER_001");
    }

    // 회원가입·로그인·이메일 인증·이메일 찾기·지역 목록·비로그인 정책 화면·헬스체크는 토큰 없이 호출됩니다.
    @Test
    void publicApisWorkWithoutToken() throws Exception {
        String[][] apis = {
                {"POST", "/api/members"},
                {"POST", "/api/members/login"},
                {"POST", "/api/members/email-verifications"},
                {"POST", "/api/members/email-verifications/confirmation"},
                {"GET", "/api/members/find-email?nickname=minji&birth=2000-03-15"},
                {"GET", "/api/regions"},
                {"GET", "/api/policies/housing"},
                {"GET", "/api/policies/card-news/guest"},
                {"GET", "/api/health"},
        };
        for (String[] api : apis) {
            HttpResponse<String> response = send(api[0], api[1], null);

            // 요청 본문이 없어 400이 나는 API도 있지만, 인증 때문에 막히지(COMMON_002) 않으면 됩니다.
            assertThat(response.body()).as(api[0] + " " + api[1]).doesNotContain("COMMON_002");
        }
    }

    // 브라우저는 다른 주소(프론트 5173)로 요청하기 전에 OPTIONS로 허용 여부를 먼저 묻습니다(preflight).
    // 이 요청에는 토큰이 없으므로, 로그인이 필요한 API에서도 막히지 않아야 합니다.
    @Test
    void allowsCorsPreflightFromFrontendDevServer() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/favorite"))
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type")
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
    }

    private HttpResponse<String> send(String method, String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
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
