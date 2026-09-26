package com.hyeja.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.e2e.support.ApiE2eTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FavoriteApiE2eTest extends ApiE2eTestSupport {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Test
    void createsListsRejectsDuplicateAndDeletesFavorite() throws Exception {
        savePolicy(
                "FAVORITE-POLICY", "청년 월세 지원", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(10), 100, true);
        Session session = signupAndLogin();

        ApiHttpResponse created = request(
                "POST", "/api/favorite/FAVORITE-POLICY", null, session.accessToken());
        ApiHttpResponse listed = request(
                "GET", "/api/favorite?page=0&size=8", null, session.accessToken());
        ApiHttpResponse duplicate = request(
                "POST", "/api/favorite/FAVORITE-POLICY", null, session.accessToken());

        assertThat(created.status()).isEqualTo(200);
        assertThat(created.body().path("isSuccess").asBoolean()).isTrue();
        assertThat(created.body().path("result").path("policy_id").asText())
                .isEqualTo("FAVORITE-POLICY");
        assertThat(listed.status()).isEqualTo(200);
        assertThat(listed.body().path("result").path("favorites")).hasSize(1);
        assertThat(listed.body().path("result").path("totalElements").asLong()).isEqualTo(1);
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.body().path("isSuccess").asBoolean()).isFalse();
        assertThat(duplicate.body().path("code").asText()).isEqualTo("FAVORITE_001");

        ApiHttpResponse deleted = request(
                "DELETE", "/api/favorite/FAVORITE-POLICY", null, session.accessToken());
        ApiHttpResponse afterDelete = request(
                "GET", "/api/favorite?page=0&size=8", null, session.accessToken());

        assertThat(deleted.status()).isEqualTo(200);
        assertThat(deleted.body().path("code").asText()).isEqualTo("SUCCESS_001");
        assertThat(deleted.body().path("result").isNull()).isTrue();
        assertThat(afterDelete.body().path("result").path("favorites")).isEmpty();
        assertThat(favoriteRepository.count()).isZero();
    }
}
