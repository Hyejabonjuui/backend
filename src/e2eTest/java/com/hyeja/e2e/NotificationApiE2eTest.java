package com.hyeja.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.e2e.support.ApiE2eTestSupport;
import org.junit.jupiter.api.Test;

class NotificationApiE2eTest extends ApiE2eTestSupport {

    @Test
    void generatesListsReadsAndDeletesDeadlineNotification() throws Exception {
        savePolicy(
                "DEADLINE-POLICY", "마감 예정 정책", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(7), 0, true);
        Session session = signupAndLogin();
        request("POST", "/api/favorite/DEADLINE-POLICY", null, session.accessToken());

        ApiHttpResponse generated = request(
                "POST",
                "/api/notification/admin/generate?memberId=" + session.memberId(),
                null,
                session.accessToken()
        );
        ApiHttpResponse generatedAgain = request(
                "POST",
                "/api/notification/admin/generate?memberId=" + session.memberId(),
                null,
                session.accessToken()
        );
        ApiHttpResponse listed = request(
                "GET", "/api/notification?page=0&size=8", null, session.accessToken());

        assertThat(generated.status()).isEqualTo(200);
        assertThat(generated.body().path("isSuccess").asBoolean()).isTrue();
        assertThat(generated.body().path("result").asInt()).isEqualTo(1);
        assertThat(generatedAgain.status()).isEqualTo(200);
        assertThat(generatedAgain.body().path("result").asInt()).isZero();
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("result").path("notifications")).hasSize(1);
        assertThat(listed.body().path("result").path("totalElements").asLong()).isEqualTo(1);
        var item = listed.body().path("result").path("notifications").get(0);
        assertThat(item.path("policy_id").asText()).isEqualTo("DEADLINE-POLICY");
        assertThat(item.path("read_yn").asBoolean()).isFalse();
        assertThat(item.path("apply_end_date").asText()).isEqualTo("2026-10-04");
        long notificationId = item.path("notification_id").asLong();

        ApiHttpResponse read = request(
                "PATCH",
                "/api/notification/" + notificationId + "/read",
                null,
                session.accessToken()
        );
        assertThat(read.status()).isEqualTo(200);
        assertThat(read.body().path("result").path("read_yn").asBoolean()).isTrue();

        ApiHttpResponse deleted = request(
                "DELETE",
                "/api/notification/" + notificationId,
                null,
                session.accessToken()
        );
        ApiHttpResponse afterDelete = request(
                "GET", "/api/notification?page=0&size=8", null, session.accessToken());

        assertThat(deleted.status()).isEqualTo(200);
        assertThat(deleted.body().path("result").isNull()).isTrue();
        assertThat(afterDelete.body().path("result").path("notifications")).isEmpty();
        assertThat(notificationRepository.count()).isZero();
    }
}
