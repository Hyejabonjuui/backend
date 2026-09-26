package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyEmploymentCondition;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyEligibilityEvaluator {
    private final PolicyIncomeEligibilityEvaluator incomeEvaluator;

    public List<ConditionResultDTO> evaluate(
            Policy policy, Profile profile, List<PolicyRegion> policyRegions) {
        return List.of(
                evaluateAge(policy, profile),
                evaluateRegion(policy, profile, policyRegions),
                evaluateIncome(policy, profile),
                evaluateEmployment(policy, profile),
                evaluateHouseless(policy, profile));
    }

    private ConditionResultDTO evaluateAge(Policy policy, Profile profile) {
        String condition = ageCondition(policy);
        if (hasNoAgeLimit(policy)) {
            return result(EligibilityConditionType.AGE, EligibilityStatus.Y,
                    "제한 없음", memberAge(profile));
        }
        if (profile.getBirth() == null) {
            return result(EligibilityConditionType.AGE, EligibilityStatus.U,
                    condition, "미입력");
        }

        int age = Period.between(profile.getBirth(), LocalDate.now()).getYears();
        boolean belowMinimum = policy.getMinAge() != null && age < policy.getMinAge();
        boolean aboveMaximum = policy.getMaxAge() != null && age > policy.getMaxAge();
        EligibilityStatus status = belowMinimum || aboveMaximum
                ? EligibilityStatus.N : EligibilityStatus.Y;
        if (policy.getMinAge() == null && policy.getMaxAge() == null) {
            status = EligibilityStatus.U;
        }
        return result(EligibilityConditionType.AGE, status, condition, "만 " + age + "세");
    }

    private ConditionResultDTO evaluateRegion(
            Policy policy, Profile profile, List<PolicyRegion> policyRegions) {
        if (policyRegions == null || policyRegions.isEmpty()) {
            return result(EligibilityConditionType.REGION, EligibilityStatus.Y,
                    "전국", profileRegion(profile));
        }
        String condition = policy.getRegionCondition();
        if (condition == null || condition.isBlank()) {
            condition = "확인 필요";
        }
        if (profile.getRegion() == null) {
            return result(EligibilityConditionType.REGION, EligibilityStatus.U,
                    condition, "미입력");
        }
        boolean matches = policyRegions.stream().anyMatch(policyRegion ->
                policyRegion.getRegion().getRegionCode()
                        .equals(profile.getRegion().getRegionCode()));
        return result(EligibilityConditionType.REGION,
                matches ? EligibilityStatus.Y : EligibilityStatus.N,
                condition, profileRegion(profile));
    }

    private ConditionResultDTO evaluateIncome(Policy policy, Profile profile) {
        return result(EligibilityConditionType.INCOME,
                incomeEvaluator.evaluate(policy, profile),
                incomeCondition(policy),
                profile.getIncomeRangeCode() == null
                        ? "미입력" : profile.getIncomeRangeCode().getLabel());
    }

    private ConditionResultDTO evaluateEmployment(Policy policy, Profile profile) {
        Set<PolicyEmploymentCondition> conditions = policy.getEmploymentCodes();
        if (conditions == null || conditions.isEmpty()) {
            return result(EligibilityConditionType.EMPLOYMENT, EligibilityStatus.U,
                    "정보 없음", employmentValue(profile));
        }
        if (conditions.contains(PolicyEmploymentCondition.NO_RESTRICTION)) {
            return result(EligibilityConditionType.EMPLOYMENT, EligibilityStatus.Y,
                    "취업 상태 제한 없음", employmentValue(profile));
        }
        EmploymentStatus memberEmployment = profile.getEmploymentCode();
        if (memberEmployment == null) {
            return result(EligibilityConditionType.EMPLOYMENT, EligibilityStatus.U,
                    employmentCondition(conditions), "미입력");
        }
        boolean matches = conditions.stream()
                .anyMatch(condition -> condition.name().equals(memberEmployment.name()));
        return result(EligibilityConditionType.EMPLOYMENT,
                matches ? EligibilityStatus.Y : EligibilityStatus.N,
                employmentCondition(conditions), memberEmployment.getLabel());
    }

    private ConditionResultDTO evaluateHouseless(Policy policy, Profile profile) {
        if (policy.getHouselessYn() == null) {
            return result(EligibilityConditionType.HOUSELESS, EligibilityStatus.U,
                    "확인 필요", houselessValue(profile));
        }
        if (Boolean.FALSE.equals(policy.getHouselessYn())) {
            return result(EligibilityConditionType.HOUSELESS, EligibilityStatus.Y,
                    "무주택 제한 없음", houselessValue(profile));
        }
        if (profile.getHouselessYn() == null) {
            return result(EligibilityConditionType.HOUSELESS, EligibilityStatus.U,
                    "무주택자", "미입력");
        }
        return result(EligibilityConditionType.HOUSELESS,
                profile.getHouselessYn() ? EligibilityStatus.Y : EligibilityStatus.N,
                "무주택자", houselessValue(profile));
    }

    private String ageCondition(Policy policy) {
        if (hasNoAgeLimit(policy)) return "제한 없음";
        if (policy.getMinAge() != null && policy.getMaxAge() != null) {
            return "만 " + policy.getMinAge() + "~" + policy.getMaxAge() + "세";
        }
        if (policy.getMinAge() != null) return "만 " + policy.getMinAge() + "세 이상";
        if (policy.getMaxAge() != null) return "만 " + policy.getMaxAge() + "세 이하";
        return "확인 필요";
    }

    private boolean hasNoAgeLimit(Policy policy) {
        if (Boolean.FALSE.equals(policy.getAgeLimitYn())) return true;
        return policy.getMinAge() != null && policy.getMinAge() <= 0
                && policy.getMaxAge() != null && policy.getMaxAge() <= 0;
    }

    private String memberAge(Profile profile) {
        return profile.getBirth() == null ? "미입력"
                : "만 " + Period.between(profile.getBirth(), LocalDate.now()).getYears() + "세";
    }

    private String profileRegion(Profile profile) {
        return profile.getRegion() == null ? "미입력" : profile.getRegion().getSigunguName();
    }

    private String incomeCondition(Policy policy) {
        PolicyIncomeCondition condition = policy.getIncomeConditionCode();
        if (condition == null || condition == PolicyIncomeCondition.UNKNOWN) return "확인 필요";
        if (condition == PolicyIncomeCondition.NO_RESTRICTION) return "소득 제한 없음";
        if (condition == PolicyIncomeCondition.CONDITIONAL) {
            return policy.getIncomeEtc() == null ? "복합 소득 조건" : policy.getIncomeEtc();
        }
        String min = policy.getIncomeMin() == null ? null : money(policy.getIncomeMin()) + " 이상";
        String max = policy.getIncomeMax() == null ? null : money(policy.getIncomeMax()) + " 이하";
        if (min != null && max != null) return min + " ~ " + max;
        return min != null ? min : max != null ? max : "확인 필요";
    }

    private String employmentCondition(Set<PolicyEmploymentCondition> conditions) {
        return conditions.stream()
                .map(PolicyEmploymentCondition::getLabel)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String employmentValue(Profile profile) {
        return profile.getEmploymentCode() == null
                ? "미입력" : profile.getEmploymentCode().getLabel();
    }

    private String houselessValue(Profile profile) {
        if (profile.getHouselessYn() == null) return "미입력";
        return profile.getHouselessYn() ? "무주택" : "주택 소유";
    }

    private String money(Integer value) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원";
    }

    private ConditionResultDTO result(EligibilityConditionType type, EligibilityStatus status,
            String policyCondition, String memberValue) {
        return new ConditionResultDTO(type, status, policyCondition, memberValue);
    }
}
