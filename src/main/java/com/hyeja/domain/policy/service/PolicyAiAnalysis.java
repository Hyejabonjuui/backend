package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;

public record PolicyAiAnalysis(
        PolicyCategory category,
        double categoryConfidence,
        String categoryReason,
        Boolean houselessYn,
        double houselessConfidence,
        String houselessReason,
        PolicyIncomeCondition incomeCondition,
        Integer incomeMin,
        Integer incomeMax,
        double incomeConfidence,
        String incomeReason) {
}
