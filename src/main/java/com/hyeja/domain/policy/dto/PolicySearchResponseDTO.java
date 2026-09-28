package com.hyeja.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyApplyPeriod;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record PolicySearchResponseDTO(
        int approvedCount,
        int underReviewCount,
        int declinedCount,
        List<PolicySearchItemDTO> approved,
        List<PolicySearchItemDTO> underReview,
        List<PolicySearchItemDTO> declined) {

    public PolicySearchResponseDTO(
            List<PolicySearchItemDTO> approved,
            List<PolicySearchItemDTO> underReview,
            List<PolicySearchItemDTO> declined) {
        this(approved.size(), underReview.size(), declined.size(), approved, underReview, declined);
    }

    public record PolicySearchItemDTO(
            String policyId,
            String policyName,
            Set<PolicyCategory> categories,
            LocalDate applyEndDate,
            PolicyApplyPeriod applyPeriod,
            @JsonProperty("isFavorite") boolean isFavorite,
            String aiReason,
            PolicyEligibilityStatusDTO status) {
    }

    public record PolicyEligibilityStatusDTO(
            EligibilityStatus age,
            EligibilityStatus region,
            EligibilityStatus income,
            EligibilityStatus employment,
            EligibilityStatus houseless) {
    }
}
