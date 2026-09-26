package com.hyeja.domain.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void filtersAndPaginatesGuestPoliciesWithAlwaysOpenPoliciesLast() {
        LocalDate today = LocalDate.of(2026, 9, 27);
        persistPolicy("soon", "마감 임박", PolicyCategory.MONTHLY_RENT,
                today.plusDays(1), 10, true);
        persistPolicy("later", "마감 여유", PolicyCategory.MONTHLY_RENT,
                today.plusDays(5), 20, true);
        persistPolicy("always", "상시 모집", PolicyCategory.MONTHLY_RENT,
                null, 30, true);
        persistPolicy("expired", "마감됨", PolicyCategory.MONTHLY_RENT,
                today.minusDays(1), 40, true);
        persistPolicy("inactive", "비활성", PolicyCategory.MONTHLY_RENT,
                today.plusDays(2), 50, false);
        Policy deleted = persistPolicy("deleted", "삭제됨", PolicyCategory.MONTHLY_RENT,
                today.plusDays(3), 60, true);
        deleted.softDelete();
        persistPolicy("other-category", "전세 정책", PolicyCategory.JEONSE,
                today.plusDays(1), 70, true);
        entityManager.flush();
        entityManager.clear();

        var firstPage = policyRepository.findGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT,
                today,
                PageRequest.of(0, 2, PolicySort.DEADLINE.toSort())
        );
        var secondPage = policyRepository.findGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT,
                today,
                PageRequest.of(1, 2, PolicySort.DEADLINE.toSort())
        );

        assertThat(firstPage.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("soon", "later");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("always");
    }

    @Test
    void appliesViewCountAndNameSortsWithStablePolicyIdOrder() {
        LocalDate today = LocalDate.of(2026, 9, 27);
        persistPolicy("alpha", "Alpha", PolicyCategory.MONTHLY_RENT,
                today.plusDays(2), 20, true);
        persistPolicy("beta", "Beta", PolicyCategory.JEONSE,
                today.plusDays(2), 10, true);
        persistPolicy("gamma", "Gamma", PolicyCategory.OTHER,
                today.plusDays(2), 20, true);
        entityManager.flush();
        entityManager.clear();

        var byViewCount = policyRepository.findGuestHousingPolicies(
                null,
                today,
                PageRequest.of(0, 8, PolicySort.VIEW_COUNT.toSort())
        );
        var byName = policyRepository.findGuestHousingPolicies(
                null,
                today,
                PageRequest.of(0, 8, PolicySort.NAME.toSort())
        );

        assertThat(byViewCount.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("alpha", "gamma", "beta");
        assertThat(byName.getContent())
                .extracting(Policy::getPolicyId)
                .containsExactly("alpha", "beta", "gamma");
    }

    private Policy persistPolicy(
            String policyId,
            String policyName,
            PolicyCategory category,
            LocalDate applyEndDate,
            int viewCount,
            boolean active
    ) {
        Policy policy = Policy.builder()
                .policyId(policyId)
                .policyName(policyName)
                .category(category)
                .ageLimitYn(false)
                .applyPeriodCode(applyEndDate == null ? "ALWAYS" : "PERIOD")
                .applyEndDate(applyEndDate)
                .viewCount(viewCount)
                .activeYn(active)
                .build();
        entityManager.persist(policy);
        return policy;
    }
}
