package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyHouselessRequirement;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import java.util.Set;

public record PolicyAiAnalysis(
        String description,
        Set<PolicyCategory> categories,
        double categoryConfidence,
        String categoryReason,
        PolicyHouselessRequirement houselessRequirement,
        double houselessConfidence,
        String houselessReason,
        PolicyIncomeCondition incomeCondition,
        Integer incomeMin,
        Integer incomeMax,
        double incomeConfidence,
        String incomeReason) {
}
