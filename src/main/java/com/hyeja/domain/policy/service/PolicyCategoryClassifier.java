package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;

public interface PolicyCategoryClassifier {
    PolicyCategoryClassification classify(PolicyItem item);
}
