package com.hyeja.domain.member.controller;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.dto.MemberFindEmailResponseDTO;
import com.hyeja.domain.member.dto.MemberLoginRequestDTO;
import com.hyeja.domain.member.dto.MemberLoginResponseDTO;
import com.hyeja.domain.member.dto.MemberSignupRequestDTO;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원", description = "회원 계정 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원가입 (S-03 계정 정보 + S-04 내 조건을 한 번에) — 예: POST /api/members
    // 토큰은 발급하지 않습니다. 인증(JWT) 작업 이후 로그인에서 발급합니다.
    @Operation(
            summary = "회원가입",
            description = "계정 정보(이메일·비밀번호·닉네임)와 내 조건(profile)을 한 번에 받아 함께 저장합니다. "
                    + "조건 없이는 가입할 수 없으며, 하나라도 실패하면 회원·조건 모두 저장되지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "목록에 없는 코드 (COMMON_001) / 형식 오류·필수 항목 누락·미래 생년월일 (COMMON_003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 지역 (REGION_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 이메일 (MEMBER_002) / 이미 사용 중인 닉네임 (MEMBER_003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("")
    public ApiResponse<MemberAccountResponseDTO> signup(
            @Valid @RequestBody MemberSignupRequestDTO request
    ) {
        MemberAccountResponseDTO result = memberService.signup(request);
        return ApiResponse.onSuccess(result);
    }

    // 내 계정 조회 (마이페이지 S-08 계정 탭) — 예: GET /api/members/me?memberId=1
    // TODO: 인증(JWT) 기반이 생기면 memberId 쿼리 파라미터를 없애고 토큰에서 회원을 식별합니다.
    //       그 전까지는 memberId만 알면 누구의 계정이든 조회되므로 임시 방식입니다.
    @GetMapping("/me")
    public ApiResponse<MemberAccountResponseDTO> getMyAccount(
            @Parameter(
                    name = "memberId",
                    description = "조회할 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            )
            @RequestParam(name = "memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId
    ) {
        MemberAccountResponseDTO result = memberService.getMyAccount(memberId);
        return ApiResponse.onSuccess(result);
    }

    // 이메일 찾기 (로그인 모달의 '이메일 찾기') — 예: GET /api/members/find-email?nickname=민지&birth=2000-03-15
    // 비로그인 상태에서 호출합니다. 찾지 못하면 이유를 구분하지 않고 MEMBER_004 하나로 응답합니다.
    @Operation(
            summary = "이메일 찾기",
            description = "닉네임과 생년월일이 일치하는 회원의 이메일을 가려서(@ 앞 3글자만 표시) 가입일과 함께 반환합니다. "
                    + "닉네임이 없거나 생년월일이 다르거나 탈퇴한 회원이면 모두 MEMBER_004로 응답합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이메일 찾기 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "닉네임·생년월일 누락 또는 날짜 형식 오류 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가입된 정보 없음 (MEMBER_004)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/find-email")
    public ApiResponse<MemberFindEmailResponseDTO> findEmail(
            @Parameter(name = "nickname", description = "닉네임", in = ParameterIn.QUERY, example = "민지", required = true)
            @RequestParam(name = "nickname") String nickname,
            @Parameter(name = "birth", description = "생년월일 (yyyy-MM-dd)", in = ParameterIn.QUERY,
                    example = "2000-03-15", required = true)
            @RequestParam(name = "birth") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birth
    ) {
        return ApiResponse.onSuccess(memberService.findEmail(nickname, birth));
    }

    // 회원 탈퇴 (마이페이지 S-08 계정 탭) — 예: PATCH /api/members/me/delete?memberId=1
    // 행을 지우지 않고 deleted_at을 찍는 soft delete라 DELETE가 아니라 PATCH입니다 (팀 확정 경로).
    // TODO: 인증(JWT) 기반이 생기면 memberId 쿼리 파라미터를 없애고 토큰에서 회원을 식별합니다.
    @Operation(
            summary = "회원 탈퇴",
            description = "회원·내 조건은 soft delete(deleted_at 기록), 관심 정책·알림은 삭제합니다. 되돌릴 수 없습니다. "
                    + "탈퇴한 이메일·닉네임으로는 다시 가입할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "memberId 누락 또는 양수가 아님 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 이미 탈퇴한 회원 (MEMBER_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping("/me/delete")
    public ApiResponse<Void> withdraw(
            @Parameter(
                    name = "memberId",
                    description = "탈퇴할 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            )
            @RequestParam(name = "memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId
    ) {
        memberService.withdraw(memberId);
        return ApiResponse.onSuccess(null);
    }

    // 로그인 — 예: POST /api/members/login
    @Operation(
            summary = "로그인",
            description = "이메일·비밀번호가 맞으면 accessToken(30분 유효)을 발급합니다. "
                    + "이후 요청 헤더에 Authorization: Bearer <accessToken>으로 보냅니다. "
                    + "이메일이 없거나 비밀번호가 틀리거나 탈퇴한 회원이면 모두 MEMBER_005로 응답합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이메일·비밀번호 누락 (COMMON_003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "이메일 또는 비밀번호 불일치 (MEMBER_005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/login")
    public ApiResponse<MemberLoginResponseDTO> login(@Valid @RequestBody MemberLoginRequestDTO request) {
        return ApiResponse.onSuccess(memberService.login(request));
    }

    // 로그아웃 — 예: POST /api/members/logout (헤더 Authorization: Bearer <accessToken>)
    // 토큰이 없거나 잘못됐거나 이미 로그아웃한 토큰이면 SecurityConfig가 컨트롤러 전에 401(COMMON_002)로 막습니다.
    @Operation(
            summary = "로그아웃",
            description = "요청 헤더의 토큰을 무효화합니다. 같은 토큰으로 다시 요청하면 401입니다. "
                    + "Swagger에서는 오른쪽 위 Authorize 버튼에 로그인으로 받은 accessToken을 넣고 호출합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "토큰 없음·잘못된 토큰·만료·이미 로그아웃한 토큰 (COMMON_002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Parameter(hidden = true) Authentication authentication) {
        // 인증 필터가 credentials에 토큰 원문을 넣어 둡니다.
        memberService.logout((String) authentication.getCredentials());
        return ApiResponse.onSuccess(null);
    }
}
