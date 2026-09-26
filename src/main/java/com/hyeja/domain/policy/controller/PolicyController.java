package com.hyeja.domain.policy.controller;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책", description = "정책 관련 API")
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @Operation(
            summary = "주거 정책 동기화",
            description = "외부 정책 API에서 청년 주거 정책을 조회하여 데이터베이스에 동기화합니다."
    )
    @PostMapping("/sync")
    public ApiResponse<String> syncPolicies() {
        int savedCount = policyService.fetchAndSaveHousingPolicies();
        return ApiResponse.onSuccess(
                "온통청년 주거 정책 " + savedCount + "건 동기화가 완료되었습니다.");
    }

    @Operation(
            summary = "주거 정책 목록 조회",
            description = "데이터베이스에 저장된 주거 정책 목록을 조회합니다."
    )
    @GetMapping("/housing")
    public ApiResponse<List<Policy>> getHousingPolicies() {
        return ApiResponse.onSuccess(policyService.getHousingPolicies());
    }

    @Operation(
            summary = "정책 상세 조회",
            description = "정책 ID와 회원 ID를 기준으로 회원 맞춤 정보를 포함한 정책 상세 내용을 조회합니다."
    )
    @GetMapping("/{policyId}/{memberId}")
    public ApiResponse<PolicyDetailResponseDTO> getPolicyDetailForMember(
            @PathVariable("policyId") String policyId,
            @PathVariable("memberId") Long memberId) {
        return ApiResponse.onSuccess(
                policyService.getPolicyDetailForMember(policyId, memberId));
    }
}
