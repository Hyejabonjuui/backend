package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.profile.entity.Profile;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PolicySearchAiAnalyzer {
    SearchIntent analyzeIntent(String query);

    Map<String, String> generateReasons(List<ReasonRequest> requests);

    record SearchIntent(boolean housingRelated, Set<PolicyCategory> categories) {
    }

    record ReasonRequest(
            Policy policy,
            Profile profile,
            EligibilityStatus overallStatus,
            List<ConditionResultDTO> conditions) {
    }
}
