package com.hyeja.domain.policy.ctrl;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // 1. 외부 API 데이터를 당겨와서 DB에 적재하는 수동 트리거 API
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncPolicies() {
        policyService.fetchAndSaveHousingPolicies();
        return ResponseEntity.ok(ApiResponse.onSuccess("온통청년 주거 정책 데이터 동기화가 성공적으로 완료되었습니다."));
    }

    // 2. 적재된 주거 정책 목록 조회 API
    @GetMapping("/housing")
    public ResponseEntity<ApiResponse<List<Policy>>> getHousingPolicies() {
        List<Policy> housingPolicies = policyService.getHousingPolicies();
        return ResponseEntity.ok(ApiResponse.onSuccess(housingPolicies));
    }
}