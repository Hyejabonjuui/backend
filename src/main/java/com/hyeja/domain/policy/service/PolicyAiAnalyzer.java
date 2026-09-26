package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;

public interface PolicyAiAnalyzer {
    PolicyAiAnalysis analyze(PolicyItem item);
}
