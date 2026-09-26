package com.hyeja.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.e2e.support.ApiE2eTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PolicyApiE2eTest extends ApiE2eTestSupport {

    @Autowired
    private PolicyRegionRepository policyRegionRepository;

    @Test
    void filtersSortsAndPaginatesPublicHousingPolicies() throws Exception {
        Region region = saveDefaultRegion();
        Policy soon = savePolicy(
                "POLICY-1", "Bravo", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(2), 10, true);
        savePolicy(
                "POLICY-2", "Charlie", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(5), 30, true);
        savePolicy(
                "POLICY-3", "Alpha", PolicyCategory.MONTHLY_RENT,
                null, 20, true);
        savePolicy(
                "POLICY-4", "다른 카테고리", PolicyCategory.JEONSE,
                FIXED_TODAY.plusDays(1), 40, true);
        savePolicy(
                "POLICY-5", "비활성 정책", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(1), 50, false);
        savePolicy(
                "POLICY-6", "마감 정책", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.minusDays(1), 60, true);
        Policy deleted = savePolicy(
                "POLICY-7", "삭제 정책", PolicyCategory.MONTHLY_RENT,
                FIXED_TODAY.plusDays(1), 70, true);
        deleted.softDelete();
        policyRepository.saveAndFlush(deleted);
        policyRegionRepository.saveAndFlush(PolicyRegion.builder()
                .policy(soon)
                .region(region)
                .build());

        ApiHttpResponse firstPage = request(
                "GET",
                "/api/policies/housing?category=MONTHLY_RENT&sort=DEADLINE&page=0&size=2",
                null,
                null
        );
        ApiHttpResponse secondPage = request(
                "GET",
                "/api/policies/housing?category=MONTHLY_RENT&sort=DEADLINE&page=1&size=2",
                null,
                null
        );
        ApiHttpResponse byViews = request(
                "GET",
                "/api/policies/housing?category=MONTHLY_RENT&sort=VIEW_COUNT&page=0&size=10",
                null,
                null
        );
        ApiHttpResponse byName = request(
                "GET",
                "/api/policies/housing?category=MONTHLY_RENT&sort=NAME&page=0&size=10",
                null,
                null
        );

        assertThat(firstPage.status()).isEqualTo(200);
        assertThat(firstPage.body().path("isSuccess").asBoolean()).isTrue();
        JsonNode page = firstPage.body().path("result");
        assertThat(policyIds(page)).containsExactly("POLICY-1", "POLICY-2");
        assertThat(page.path("page").asInt()).isZero();
        assertThat(page.path("size").asInt()).isEqualTo(2);
        assertThat(page.path("totalElements").asLong()).isEqualTo(3);
        assertThat(page.path("totalPages").asInt()).isEqualTo(2);
        assertThat(page.path("hasNext").asBoolean()).isTrue();
        assertThat(page.path("policies").get(0).path("regions").get(0).path("region_code").asText())
                .isEqualTo(REGION_CODE);
        assertThat(page.path("policies").get(0).path("nationwide").asBoolean()).isFalse();
        assertThat(page.path("policies").get(0).path("d_day").asInt()).isEqualTo(2);

        assertThat(secondPage.status()).isEqualTo(200);
        assertThat(policyIds(secondPage.body().path("result"))).containsExactly("POLICY-3");
        assertThat(secondPage.body().path("result").path("policies").get(0)
                .path("apply_end_date").isNull()).isTrue();
        assertThat(secondPage.body().path("result").path("policies").get(0)
                .path("d_day").isNull()).isTrue();

        assertThat(policyIds(byViews.body().path("result")))
                .containsExactly("POLICY-2", "POLICY-3", "POLICY-1");
        assertThat(policyIds(byName.body().path("result")))
                .containsExactly("POLICY-3", "POLICY-1", "POLICY-2");
    }

    private List<String> policyIds(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.path("policies").forEach(policy -> ids.add(policy.path("policy_id").asText()));
        return ids;
    }
}
