package com.hyeja.domain.policy.controller;

import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주거 정책", description = "주거 정책 동기화·조회 API")
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // 1. 외부 API 데이터를 당겨와서 DB에 적재하는 수동 트리거 API
    @PostMapping("/sync")
    public ApiResponse<String> syncPolicies() {
        policyService.fetchAndSaveHousingPolicies();
        return ApiResponse.onSuccess("온통청년 주거 정책 데이터 동기화가 성공적으로 완료되었습니다.");
    }

    // 2. 적재된 주거 정책 목록 조회 API
    @GetMapping("/housing")
    public ApiResponse<List<Policy>> getHousingPolicies() {
        List<Policy> housingPolicies = policyService.getHousingPolicies();
        return ApiResponse.onSuccess(housingPolicies);
    }

    @Operation(
            summary = "로그인 회원용 주거 정책 목록 조회",
            description = "진행 중인 주거 정책을 카테고리·정렬 조건으로 8개씩 조회합니다. "
                    + "onlyEligible이 true이면 회원의 필수 조건(지역·나이·취업·무주택)에 맞는 정책만 반환합니다. "
                    + "현재는 인증 도입 전이므로 memberId를 쿼리 파라미터로 받습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주거 정책 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "회원 ID 누락·잘못된 필터·페이지 조건 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001) / 등록된 조건 없음 (PROFILE_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/housing/me")
    public ApiResponse<PolicyListDTO> getHousingPoliciesForMember(
            @Parameter(
                    name = "memberId",
                    description = "조회할 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            )
            @RequestParam(name = "memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId,
            @Parameter(
                    name = "category",
                    description = "정책 카테고리. 생략하면 전체",
                    in = ParameterIn.QUERY,
                    example = "MONTHLY_RENT"
            )
            @RequestParam(name = "category", required = false) PolicyCategory category,
            @Parameter(
                    name = "sort",
                    description = "정렬 기준: DEADLINE(마감일), VIEW_COUNT(조회수), NAME(정책명)",
                    in = ParameterIn.QUERY,
                    example = "DEADLINE"
            )
            @RequestParam(name = "sort", defaultValue = "DEADLINE") PolicySort sort,
            @Parameter(
                    name = "onlyEligible",
                    description = "회원의 필수 조건에 맞는 정책만 조회할지 여부",
                    in = ParameterIn.QUERY,
                    example = "false"
            )
            @RequestParam(name = "onlyEligible", defaultValue = "false") boolean onlyEligible,
            @Parameter(
                    name = "page",
                    description = "페이지 번호(0부터 시작)",
                    in = ParameterIn.QUERY,
                    example = "0"
            )
            @RequestParam(name = "page", defaultValue = "0")
            @PositiveOrZero(message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @Parameter(
                    name = "size",
                    description = "페이지당 정책 개수",
                    in = ParameterIn.QUERY,
                    example = "8"
            )
            @RequestParam(name = "size", defaultValue = "8")
            @Positive(message = "페이지 크기는 양수여야 합니다.") int size
    ) {
        return ApiResponse.onSuccess(policyService.getHousingPoliciesForMember(
                memberId, category, sort, onlyEligible, page, size));
    }
}
