package com.hyeja.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.e2e.support.ApiE2eTestSupport;
import org.junit.jupiter.api.Test;

class AuthenticationApiE2eTest extends ApiE2eTestSupport {

    @Test
    void rejectsMissingInvalidAndLoggedOutTokens() throws Exception {
        Session session = signupAndLogin();

        ApiHttpResponse missing = request("GET", "/api/members/me", null, null);
        ApiHttpResponse invalid = request("GET", "/api/members/me", null, "not-a-jwt");
        ApiHttpResponse authenticated = request(
                "GET", "/api/members/me", null, session.accessToken());

        assertUnauthorized(missing);
        assertUnauthorized(invalid);
        assertThat(authenticated.status()).isEqualTo(200);
        assertThat(authenticated.body().path("result").path("memberId").asLong())
                .isEqualTo(session.memberId());

        ApiHttpResponse logout = request(
                "POST", "/api/members/logout", null, session.accessToken());
        ApiHttpResponse afterLogout = request(
                "GET", "/api/members/me", null, session.accessToken());

        assertThat(logout.status()).isEqualTo(200);
        assertThat(logout.body().path("isSuccess").asBoolean()).isTrue();
        assertThat(logout.body().path("code").asText()).isEqualTo("SUCCESS_001");
        assertThat(logout.body().path("result").isNull()).isTrue();
        assertUnauthorized(afterLogout);
    }

    private void assertUnauthorized(ApiHttpResponse response) {
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.body().path("isSuccess").asBoolean()).isFalse();
        assertThat(response.body().path("code").asText()).isEqualTo("COMMON_002");
        assertThat(response.body().path("message").asText()).isEqualTo("인증이 필요합니다.");
        assertThat(response.body().path("result").isNull()).isTrue();
    }
}
