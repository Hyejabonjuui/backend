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
import com.hyeja.domain.policy.enums.PolicyHouselessRequirement;
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
        when(policy.getHouselessRequirement())
                .thenReturn(PolicyHouselessRequirement.REQUIRED);

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
                .containsOnly(EligibilityStatus.ABLE);
    }

    @Test
    void displaysNoRestrictionForPolicyWithoutAgeLimit() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(false);
        Profile profile = mock(Profile.class);
        when(profile.getBirth()).thenReturn(LocalDate.now().minusYears(27));

        ConditionResultDTO result = evaluator.evaluate(policy, profile, List.of()).stream()
                .filter(condition -> condition.type() == EligibilityConditionType.AGE)
                .findFirst().orElseThrow();

        assertThat(result.status()).isEqualTo(EligibilityStatus.ABLE);
        assertThat(result.policyCondition()).isEqualTo("제한 없음");
    }

    @Test
    void treatsLegacyZeroAgeRangeAsNoRestriction() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(true);
        when(policy.getMinAge()).thenReturn(0);
        when(policy.getMaxAge()).thenReturn(0);
        Profile profile = mock(Profile.class);
        when(profile.getBirth()).thenReturn(LocalDate.now().minusYears(27));

        ConditionResultDTO result = evaluator.evaluate(policy, profile, List.of()).stream()
                .filter(condition -> condition.type() == EligibilityConditionType.AGE)
                .findFirst().orElseThrow();

        assertThat(result.status()).isEqualTo(EligibilityStatus.ABLE);
        assertThat(result.policyCondition()).isEqualTo("제한 없음");
    }

    @Test
    void displaysRegionNameFromPolicyRegion() {
        Policy policy = mock(Policy.class);
        Region region = mock(Region.class);
        when(region.getRegionCode()).thenReturn("11680");
        when(region.getSigunguName()).thenReturn("서울특별시 강남구");
        com.hyeja.domain.policy.entity.PolicyRegion policyRegion =
                mock(com.hyeja.domain.policy.entity.PolicyRegion.class);
        when(policyRegion.getRegion()).thenReturn(region);
        Profile profile = mock(Profile.class);
        when(profile.getRegion()).thenReturn(region);

        ConditionResultDTO result = evaluator.evaluate(
                        policy, profile, List.of(policyRegion)).stream()
                .filter(condition -> condition.type() == EligibilityConditionType.REGION)
                .findFirst().orElseThrow();

        assertThat(result.status()).isEqualTo(EligibilityStatus.ABLE);
        assertThat(result.policyCondition()).isEqualTo("서울특별시 강남구");
    }

    @Test
    void returnsUnknownWhenPolicyOrMemberInformationIsInsufficient() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(true);
        when(policy.getMinAge()).thenReturn(null);
        when(policy.getMaxAge()).thenReturn(null);
        when(policy.getIncomeConditionCode()).thenReturn(PolicyIncomeCondition.CONDITIONAL);
        when(policy.getHouselessRequirement())
                .thenReturn(PolicyHouselessRequirement.UNKNOWN);
        Profile profile = mock(Profile.class);

        List<ConditionResultDTO> results = evaluator.evaluate(policy, profile, List.of());

        assertThat(results).filteredOn(result ->
                        result.type() != EligibilityConditionType.REGION)
                .extracting(ConditionResultDTO::status)
                .containsOnly(EligibilityStatus.UNKNOWN);
        assertThat(results).filteredOn(result ->
                        result.type() == EligibilityConditionType.REGION)
                .extracting(ConditionResultDTO::status)
                .containsExactly(EligibilityStatus.ABLE);
    }

    @Test
    void comparesPolicyAndMemberSigunguRegionCodes() {
        Policy policy = mock(Policy.class);
        when(policy.getAgeLimitYn()).thenReturn(false);
        when(policy.getIncomeConditionCode()).thenReturn(PolicyIncomeCondition.NO_RESTRICTION);
        when(policy.getEmploymentCodes()).thenReturn(
                Set.of(PolicyEmploymentCondition.NO_RESTRICTION));
        when(policy.getHouselessRequirement())
                .thenReturn(PolicyHouselessRequirement.NOT_REQUIRED);
        Region policyRegion = mock(Region.class);
        when(policyRegion.getRegionCode()).thenReturn("11440");
        when(policyRegion.getSigunguName()).thenReturn("서울특별시 마포구");
        com.hyeja.domain.policy.entity.PolicyRegion policyRegionLink =
                mock(com.hyeja.domain.policy.entity.PolicyRegion.class);
        when(policyRegionLink.getRegion()).thenReturn(policyRegion);

        Region matchingRegion = mock(Region.class);
        when(matchingRegion.getRegionCode()).thenReturn("11440");
        when(matchingRegion.getSigunguName()).thenReturn("서울특별시 마포구");
        Profile matchingProfile = mock(Profile.class);
        when(matchingProfile.getRegion()).thenReturn(matchingRegion);

        Region differentRegion = mock(Region.class);
        when(differentRegion.getRegionCode()).thenReturn("11680");
        when(differentRegion.getSigunguName()).thenReturn("서울특별시 강남구");
        Profile differentProfile = mock(Profile.class);
        when(differentProfile.getRegion()).thenReturn(differentRegion);

        ConditionResultDTO matchingResult = evaluator.evaluate(
                        policy, matchingProfile, List.of(policyRegionLink)).stream()
                .filter(result -> result.type() == EligibilityConditionType.REGION)
                .findFirst().orElseThrow();
        ConditionResultDTO differentResult = evaluator.evaluate(
                        policy, differentProfile, List.of(policyRegionLink)).stream()
                .filter(result -> result.type() == EligibilityConditionType.REGION)
                .findFirst().orElseThrow();

        assertThat(matchingResult.status()).isEqualTo(EligibilityStatus.ABLE);
        assertThat(matchingResult.policyCondition()).isEqualTo("서울특별시 마포구");
        assertThat(matchingResult.memberValue()).isEqualTo("서울특별시 마포구");
        assertThat(differentResult.status()).isEqualTo(EligibilityStatus.DISABLE);
        assertThat(differentResult.memberValue()).isEqualTo("서울특별시 강남구");
    }

    @Test
    void matchesSidoPolicyCodeWithMemberSigunguPrefix() {
        Policy policy = mock(Policy.class);
        Region sido = mock(Region.class);
        when(sido.getRegionCode()).thenReturn("11000");
        when(sido.getSigunguName()).thenReturn("서울특별시");
        com.hyeja.domain.policy.entity.PolicyRegion policyRegion =
                mock(com.hyeja.domain.policy.entity.PolicyRegion.class);
        when(policyRegion.getRegion()).thenReturn(sido);

        Region memberRegion = mock(Region.class);
        when(memberRegion.getRegionCode()).thenReturn("11680");
        when(memberRegion.getSigunguName()).thenReturn("서울특별시 강남구");
        Profile profile = mock(Profile.class);
        when(profile.getRegion()).thenReturn(memberRegion);

        ConditionResultDTO result = evaluator.evaluate(policy, profile, List.of(policyRegion))
                .stream()
                .filter(condition -> condition.type() == EligibilityConditionType.REGION)
                .findFirst().orElseThrow();

        assertThat(result.status()).isEqualTo(EligibilityStatus.ABLE);
        assertThat(result.policyCondition()).isEqualTo("서울특별시");
    }
}
