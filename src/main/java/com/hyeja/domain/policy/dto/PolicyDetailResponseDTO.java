package com.hyeja.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyApplyPeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record PolicyDetailResponseDTO(
        String policyId,
        String policyName,
        Set<PolicyCategory> categories,
        List<String> categoryLabels,
        String apiSubCategory,
        String keywords,
        String description,
        String supportContent,
        String extraQualification,
        PolicyApplyPeriod applyPeriod,
        String applyPeriodLabel,
        LocalDate applyStartDate,
        LocalDate applyEndDate,
        String applyMethod,
        String applyUrl,
        String refUrl,
        Boolean activeYn,
        @JsonProperty("isFavorite") boolean isFavorite,
        EligibilityStatus overallStatus,
        List<ConditionResultDTO> conditions) {

    public record ConditionResultDTO(
            EligibilityConditionType type,
            EligibilityStatus status,
            String policyCondition,
            String memberValue) {
    }
}
