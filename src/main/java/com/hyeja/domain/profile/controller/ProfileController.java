package com.hyeja.domain.profile.controller;

import com.hyeja.domain.profile.dto.ProfileRequestDTO;
import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// URL은 명세대로 /members/me/profile 이지만, 다루는 데이터가 Profile이라 profile 도메인에 둡니다.
@Tag(name = "내 조건", description = "회원 조건(프로필) API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 내 조건 조회 (마이페이지 S-08 내 조건 탭, 추천 시 자동 로드) — 예: GET /api/members/me/profile
    // memberId는 인증 필터가 토큰에서 꺼낸 회원 ID입니다. 토큰이 없으면 SecurityConfig가 401로 막습니다.
    @Operation(
            summary = "내 조건 조회",
            description = "회원 ID로 등록된 조건(생년월일·거주지·취업 상태 등)을 조회합니다. "
                    + "코드(enum 이름)와 한글 이름을 함께 내려주며, 등록하지 않은 선택 항목은 null입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 조건 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001) / 등록된 조건 없음 (PROFILE_001, 온보딩으로 이동)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/me/profile")
    public ApiResponse<ProfileResponseDTO> getMyProfile(
            @AuthenticationPrincipal Long memberId
    ) {
        ProfileResponseDTO result = profileService.getMyProfile(memberId);
        return ApiResponse.onSuccess(result);
    }

    // 내 조건 수정 (마이페이지 S-08 내 조건 탭) — 예: PATCH /api/members/me/profile
    @Operation(
            summary = "내 조건 수정",
            description = "회원 ID로 내 조건 8개를 전체 교체합니다. 선택 항목(혼인·소득·학력·주거 형태)을 null로 보내면 기존 값이 지워집니다. "
                    + "응답은 내 조건 조회와 같은 형식입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 조건 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "목록에 없는 코드 (COMMON_001) / 필수 항목 누락·미래 생년월일 (COMMON_003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001) / 조건 없음 (PROFILE_001) / 존재하지 않는 지역 (REGION_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping("/me/profile")
    public ApiResponse<ProfileResponseDTO> updateMyProfile(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ProfileRequestDTO request
    ) {
        ProfileResponseDTO result = profileService.updateMyProfile(memberId, request);
        return ApiResponse.onSuccess(result);
    }
}
