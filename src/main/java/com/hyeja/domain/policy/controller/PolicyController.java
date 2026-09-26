package com.hyeja.domain.policy.controller;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping("/sync")
    public ApiResponse<String> syncPolicies() {
        int savedCount = policyService.fetchAndSaveHousingPolicies();
        return ApiResponse.onSuccess(
                "온통청년 주거 정책 " + savedCount + "건 동기화가 완료되었습니다.");
    }

    @GetMapping("/housing")
    public ApiResponse<List<Policy>> getHousingPolicies() {
        return ApiResponse.onSuccess(policyService.getHousingPolicies());
    }

    @GetMapping("/{policyId}/{memberId}")
    public ApiResponse<PolicyDetailResponseDTO> getPolicyDetailForMember(
            @PathVariable("policyId") String policyId,
            @PathVariable("memberId") Long memberId) {
        return ApiResponse.onSuccess(
                policyService.getPolicyDetailForMember(policyId, memberId));
    }
}
