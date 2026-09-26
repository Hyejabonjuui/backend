package com.hyeja.domain.member.ctrl;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.dto.MemberSignupRequestDTO;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    @PostMapping
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
}
