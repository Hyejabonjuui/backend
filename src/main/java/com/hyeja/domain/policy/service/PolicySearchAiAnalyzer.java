package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PolicySearchAiAnalyzer {
    SearchIntent analyzeIntent(String query);

    Map<String, String> generateReasons(List<ReasonRequest> requests);

    record SearchIntent(boolean housingRelated, Set<PolicyCategory> categories) {
    }

    record ReasonRequest(
            String policyId,
            String policyName,
            EligibilityStatus overallStatus,
            List<ConditionResultDTO> conditions) {
    }

    enum FailureType {
        UNAVAILABLE,
        EMPTY_RESPONSE,
        INVALID_RESPONSE
    }

    final class AnalysisException extends RuntimeException {
        private final FailureType failureType;

        public AnalysisException(FailureType failureType, String message) {
            super(message);
            this.failureType = failureType;
        }

        public AnalysisException(FailureType failureType, String message, Throwable cause) {
            super(message, cause);
            this.failureType = failureType;
        }

        public FailureType getFailureType() {
            return failureType;
        }
    }
}
