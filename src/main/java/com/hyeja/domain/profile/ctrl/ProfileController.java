package com.hyeja.domain.profile.ctrl;

import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// URL은 명세대로 /members/me/profile 이지만, 다루는 데이터가 Profile이라 profile 도메인에 둡니다.
@Tag(name = "내 조건", description = "회원 조건(프로필) API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // 내 조건 조회 (마이페이지 S-08 내 조건 탭, 추천 시 자동 로드) — 예: GET /api/members/me/profile?memberId=1
    // TODO: 인증(JWT) 기반이 생기면 memberId 쿼리 파라미터를 없애고 토큰에서 회원을 식별합니다.
    //       그 전까지는 memberId만 알면 누구의 조건이든 조회되므로 임시 방식입니다.
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
                    responseCode = "400",
                    description = "회원 ID 누락 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001) / 등록된 조건 없음 (PROFILE_001, 온보딩으로 이동)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getMyProfile(
            @Parameter(description = "조회할 회원 ID", example = "1", required = true) // 추후 memberId는 없앨 예정
            @RequestParam Long memberId
    ) {
        ProfileResponseDTO result = profileService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
