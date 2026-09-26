package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.IncomeRange;
import org.junit.jupiter.api.Test;

class PolicyIncomeEligibilityEvaluatorTest {

    private final PolicyIncomeEligibilityEvaluator evaluator =
            new PolicyIncomeEligibilityEvaluator();

    @Test
    void returnsYWhenMemberRangeIsFullyWithinPolicyRange() {
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.COMPARABLE, null, 50_000_000),
                profile(IncomeRange.R4000_5000)))
                .isEqualTo(EligibilityStatus.Y);
    }

    @Test
    void returnsNWhenMemberRangeDoesNotOverlapPolicyRange() {
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.COMPARABLE, null, 30_000_000),
                profile(IncomeRange.R4000_5000)))
                .isEqualTo(EligibilityStatus.N);
    }

    @Test
    void returnsUWhenMemberRangeOnlyPartiallyOverlapsPolicyRange() {
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.COMPARABLE, null, 45_000_000),
                profile(IncomeRange.R4000_5000)))
                .isEqualTo(EligibilityStatus.U);
    }

    @Test
    void handlesNoRestrictionConditionalAndMissingProfileIncome() {
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.NO_RESTRICTION, null, null), null))
                .isEqualTo(EligibilityStatus.Y);
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.CONDITIONAL, null, null),
                profile(IncomeRange.UNDER_2000)))
                .isEqualTo(EligibilityStatus.U);
        assertThat(evaluator.evaluate(
                policy(PolicyIncomeCondition.COMPARABLE, null, 50_000_000),
                profile(null)))
                .isEqualTo(EligibilityStatus.U);
    }

    private Policy policy(
            PolicyIncomeCondition condition, Integer minimum, Integer maximum) {
        Policy policy = mock(Policy.class);
        when(policy.getIncomeConditionCode()).thenReturn(condition);
        when(policy.getIncomeMin()).thenReturn(minimum);
        when(policy.getIncomeMax()).thenReturn(maximum);
        return policy;
    }

    private Profile profile(IncomeRange incomeRange) {
        Profile profile = mock(Profile.class);
        when(profile.getIncomeRangeCode()).thenReturn(incomeRange);
        return profile;
    }
}
