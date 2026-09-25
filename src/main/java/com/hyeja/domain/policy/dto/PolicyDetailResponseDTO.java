package com.hyeja.domain.policy.dto;

import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import java.util.List;

public record PolicyDetailResponseDTO(
        String policyId,
        String policyName,
        PolicyCategory category,
        String categoryLabel,
        String apiSubCategory,
        String keywords,
        String description,
        String supportContent,
        String extraQualification,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        String applyMethod,
        String applyUrl,
        String refUrl,
        Boolean activeYn,
        EligibilityStatus overallStatus,
        List<ConditionResultDTO> conditions) {

    public record ConditionResultDTO(
            EligibilityConditionType type,
            EligibilityStatus status,
            String policyCondition,
            String memberValue) {
    }
}
