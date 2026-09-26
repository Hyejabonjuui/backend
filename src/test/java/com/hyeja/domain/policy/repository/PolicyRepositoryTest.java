package com.hyeja.domain.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyEmploymentCondition;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void filtersOngoingPoliciesByRequiredMemberConditions() {
        LocalDate today = LocalDate.of(2026, 9, 26);
        Region mapo = persistRegion("11440", "서울특별시 마포구");
        Region gangnam = persistRegion("11680", "서울특별시 강남구");

        Policy nationwide = persistPolicy(
                "nationwide", "전국 정책", today.plusDays(2), "EMPLOYED", true, 19, 39);
        Policy matching = persistPolicy(
                "matching", "마포 정책", today.plusDays(5), "EMPLOYED", true, 19, 39);
        Policy alwaysOpen = persistPolicy(
                "always-open", "상시 정책", null, "EMPLOYED", true, 19, 39);
        Policy wrongRegion = persistPolicy(
                "wrong-region", "강남 정책", today.plusDays(1), "EMPLOYED", true, 19, 39);
        persistPolicy("wrong-age", "연령 불일치", today.plusDays(1), "EMPLOYED", true, 30, 39);
        persistPolicy("wrong-job", "취업 불일치", today.plusDays(1), "UNEMPLOYED", true, 19, 39);
        persistPolicy("expired", "마감 정책", today.minusDays(1), "EMPLOYED", true, 19, 39);
        entityManager.persist(PolicyRegion.builder().policy(matching).region(mapo).build());
        entityManager.persist(PolicyRegion.builder().policy(wrongRegion).region(gangnam).build());
        entityManager.flush();
        entityManager.clear();

        var result = policyRepository.findHousingPoliciesForMember(
                PolicyCategory.MONTHLY_RENT,
                true,
                today,
                26,
                true,
                "EMPLOYED",
                "11440",
                PageRequest.of(0, 8, PolicySort.DEADLINE.toSort())
        );

        assertThat(result.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly(
                        nationwide.getPolicyId(),
                        matching.getPolicyId(),
                        alwaysOpen.getPolicyId()
                );
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void appliesViewCountAndNameSorts() {
        LocalDate today = LocalDate.of(2026, 9, 26);
        Policy beta = persistPolicy(
                "beta", "나 정책", today.plusDays(2), null, false, 19, 39);
        Policy alpha = persistPolicy(
                "alpha", "가 정책", today.plusDays(2), null, false, 19, 39);
        ReflectionTestUtils.setField(beta, "viewCount", 10);
        ReflectionTestUtils.setField(alpha, "viewCount", 20);
        entityManager.flush();
        entityManager.clear();

        var byViewCount = policyRepository.findHousingPoliciesForMember(
                null, false, today, 26, true, "EMPLOYED", "11440",
                PageRequest.of(0, 8, PolicySort.VIEW_COUNT.toSort()));
        var byName = policyRepository.findHousingPoliciesForMember(
                null, false, today, 26, true, "EMPLOYED", "11440",
                PageRequest.of(0, 8, PolicySort.NAME.toSort()));

        assertThat(byViewCount.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("alpha", "beta");
        assertThat(byName.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("alpha", "beta");
    }

    private Region persistRegion(String regionCode, String name) {
        Region region = Region.builder().regionCode(regionCode).sigunguName(name).build();
        entityManager.persist(region);
        return region;
    }

    private Policy persistPolicy(
            String policyId,
            String policyName,
            LocalDate endDate,
            String employmentCodes,
            boolean houselessYn,
            int minAge,
            int maxAge
    ) {
        Policy policy = Policy.builder()
                .policyId(policyId)
                .policyName(policyName)
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(true)
                .minAge(minAge)
                .maxAge(maxAge)
                .employmentCodes(employmentCodes == null
                        ? null : java.util.Set.of(
                                PolicyEmploymentCondition.valueOf(employmentCodes)))
                .houselessYn(houselessYn)
                .applyPeriodCode("PERIOD")
                .applyEndDate(endDate)
                .build();
        entityManager.persist(policy);
        return policy;
    }
}
