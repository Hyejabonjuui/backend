package com.hyeja.domain.policy.ctrl;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {
    private final PolicyService policyService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncPolicies() {
        int savedCount = policyService.fetchAndSaveHousingPolicies();
        return ResponseEntity.ok(ApiResponse.onSuccess(
                "온통청년 주거 정책 " + savedCount + "건 동기화가 완료되었습니다."));
    }

    @GetMapping("/housing")
    public ResponseEntity<ApiResponse<List<Policy>>> getHousingPolicies() {
        return ResponseEntity.ok(ApiResponse.onSuccess(policyService.getHousingPolicies()));
    }

    @GetMapping("/{policyId}/{memberId}")
    public ResponseEntity<ApiResponse<PolicyDetailResponseDTO>> getPolicyDetailForMember(
            @PathVariable("policyId") String policyId,
            @PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(
                policyService.getPolicyDetailForMember(policyId, memberId)));
    }
}
