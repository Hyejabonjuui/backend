package com.hyeja.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.e2e.support.ApiE2eTestSupport;
import org.junit.jupiter.api.Test;

class MemberApiE2eTest extends ApiE2eTestSupport {

    @Test
    void signsUpLogsInAndLoadsAccountAndProfile() throws Exception {
        saveDefaultRegion();

        ApiHttpResponse signup = request(
                "POST",
                "/api/members",
                signupBody("member@example.com", "민지"),
                null
        );

        assertThat(signup.status()).isEqualTo(200);
        assertThat(signup.body().path("isSuccess").asBoolean()).isTrue();
        assertThat(signup.body().path("code").asText()).isEqualTo("SUCCESS_001");
        assertThat(signup.body().path("result").path("email").asText())
                .isEqualTo("member@example.com");
        long memberId = signup.body().path("result").path("memberId").asLong();

        ApiHttpResponse login = request(
                "POST",
                "/api/members/login",
                """
                        {"email":"member@example.com","password":"%s"}
                        """.formatted(PASSWORD),
                null
        );

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().path("isSuccess").asBoolean()).isTrue();
        assertThat(login.body().path("result").path("memberId").asLong()).isEqualTo(memberId);
        String token = login.body().path("result").path("accessToken").asText();
        assertThat(token).isNotBlank();

        ApiHttpResponse account = request("GET", "/api/members/me", null, token);
        ApiHttpResponse profile = request("GET", "/api/members/me/profile", null, token);

        assertThat(account.status()).isEqualTo(200);
        assertThat(account.body().path("result").path("email").asText())
                .isEqualTo("member@example.com");
        assertThat(account.body().path("result").path("nickname").asText()).isEqualTo("민지");
        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.body().path("result").path("regionCode").asText()).isEqualTo(REGION_CODE);
        assertThat(profile.body().path("result").path("regionName").asText()).isEqualTo(REGION_NAME);
        assertThat(profile.body().path("result").path("employmentCode").asText())
                .isEqualTo("EMPLOYED");

        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(profileRepository.findById("member@example.com")).isPresent();
    }
}
