package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyEmploymentCondition;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.region.entity.Region;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PolicyEligibilityEvaluatorTest {

    private final PolicyEligibilityEvaluator evaluator = new PolicyEligibilityEvaluator(
            new PolicyIncomeEligibilityEvaluator());

    @Test
    void evaluatesFiveConditionsForMember() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(true);
        when(policy.getMinAge()).thenReturn(19);
        when(policy.getMaxAge()).thenReturn(34);
        when(policy.getIncomeConditionCode()).thenReturn(PolicyIncomeCondition.COMPARABLE);
        when(policy.getIncomeMax()).thenReturn(50_000_000);
        when(policy.getEmploymentCodes()).thenReturn(Set.of(PolicyEmploymentCondition.EMPLOYED));
        when(policy.getHouselessYn()).thenReturn(true);

        Region region = mock(Region.class);
        when(region.getSigunguName()).thenReturn("서울특별시 강남구");
        Profile profile = mock(Profile.class);
        when(profile.getBirth()).thenReturn(LocalDate.now().minusYears(27));
        when(profile.getRegion()).thenReturn(region);
        when(profile.getIncomeRangeCode()).thenReturn(IncomeRange.R3000_4000);
        when(profile.getEmploymentCode()).thenReturn(EmploymentStatus.EMPLOYED);
        when(profile.getHouselessYn()).thenReturn(true);

        List<ConditionResultDTO> results = evaluator.evaluate(policy, profile, List.of());

        assertThat(results).extracting(ConditionResultDTO::type)
                .containsExactly(
                        EligibilityConditionType.AGE,
                        EligibilityConditionType.REGION,
                        EligibilityConditionType.INCOME,
                        EligibilityConditionType.EMPLOYMENT,
                        EligibilityConditionType.HOUSELESS);
        assertThat(results).extracting(ConditionResultDTO::status)
                .containsOnly(EligibilityStatus.Y);
    }

    @Test
    void returnsUnknownWhenPolicyOrMemberInformationIsInsufficient() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(true);
        when(policy.getIncomeConditionCode()).thenReturn(PolicyIncomeCondition.CONDITIONAL);
        when(policy.getHouselessYn()).thenReturn(null);
        Profile profile = mock(Profile.class);

        List<ConditionResultDTO> results = evaluator.evaluate(policy, profile, List.of());

        assertThat(results).filteredOn(result ->
                        result.type() != EligibilityConditionType.REGION)
                .extracting(ConditionResultDTO::status)
                .containsOnly(EligibilityStatus.U);
        assertThat(results).filteredOn(result ->
                        result.type() == EligibilityConditionType.REGION)
                .extracting(ConditionResultDTO::status)
                .containsExactly(EligibilityStatus.Y);
    }
}
