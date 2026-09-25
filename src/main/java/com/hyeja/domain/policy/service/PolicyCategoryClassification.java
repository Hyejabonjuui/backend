package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.enums.PolicyCategory;

public record PolicyCategoryClassification(
        PolicyCategory category,
        double confidence,
        String reason) {
}
