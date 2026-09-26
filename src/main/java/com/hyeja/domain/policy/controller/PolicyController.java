package com.hyeja.domain.policy.controller;

import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            summary = "비로그인 주거 정책 목록 조회",
            description = "인증 없이 진행 중인 주거 정책을 카테고리·정렬 조건으로 페이지 조회합니다. "
                    + "마감일순에서는 상시 정책을 기간 지정 정책 뒤에 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비로그인 주거 정책 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 필터·정렬·페이지 조건 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/housing")
    public ApiResponse<PolicyGuestResponseDTO.PolicyListDTO> getGuestHousingPolicies(
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
            @Positive(message = "페이지 크기는 양수여야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size
    ) {
        return ApiResponse.onSuccess(
                policyService.getGuestHousingPolicies(category, sort, page, size));
    }

    @Operation(
            summary = "로그인 회원용 주거 정책 목록 조회",
            description = "로그인한 회원을 기준으로 진행 중인 주거 정책을 카테고리·정렬 조건으로 조회합니다. "
                    + "onlyEligible이 true이면 회원의 필수 조건(지역·나이·취업·무주택)에 맞는 정책만 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주거 정책 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 필터·페이지 조건 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (COMMON_002)",
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
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
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
            @Positive(message = "페이지 크기는 양수여야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.") int size
    ) {
        return ApiResponse.onSuccess(policyService.getHousingPoliciesForMember(
                memberId, category, sort, onlyEligible, page, size));
    }

    @Operation(
            summary = "정책 상세 조회",
            description = "정책 ID와 로그인한 회원(토큰)을 기준으로 회원 맞춤 정보를 포함한 정책 상세 내용을 조회합니다."
    )
    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDetailResponseDTO> getPolicyDetailForMember(
            @PathVariable("policyId") String policyId,
            @AuthenticationPrincipal Long memberId) {
        return ApiResponse.onSuccess(
                policyService.getPolicyDetailForMember(policyId, memberId));
    }
}
