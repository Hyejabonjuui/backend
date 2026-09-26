package com.hyeja.domain.member.controller;

import com.hyeja.domain.member.dto.EmailVerificationRequestDTO;
import com.hyeja.domain.member.dto.EmailVerificationResponseDTO;
import com.hyeja.domain.member.service.EmailVerificationService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 회원가입(S-03) 이메일 인증. 비로그인 상태에서 호출하므로 SecurityConfig 허용 목록에 있습니다.
// 흐름: 인증 코드 발송 → 메일로 받은 코드 확인 → 30분 안에 회원가입(POST /api/members)
@Tag(name = "회원", description = "회원 계정 API")
@RestController
@RequestMapping("/api/members/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    // 예: POST /api/members/email-verifications {"email": "hyeja@example.com"}
    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "6자리 인증 코드를 메일로 보냅니다. 코드는 5분 동안 유효하고, 같은 이메일로는 60초 뒤에 다시 보낼 수 있습니다. "
                    + "다시 보내면 새 코드로 바뀝니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발송 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이메일 형식 오류 (COMMON_003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 가입된 이메일 (MEMBER_002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "60초 안에 다시 요청 (VERIFY_004)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "메일 발송 실패 (MAIL_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("")
    public ApiResponse<EmailVerificationResponseDTO.SendDTO> send(
            @Valid @RequestBody EmailVerificationRequestDTO.SendDTO request
    ) {
        long expiresInSeconds = emailVerificationService.send(request.getEmail());
        return ApiResponse.onSuccess(new EmailVerificationResponseDTO.SendDTO(expiresInSeconds));
    }

    // 예: POST /api/members/email-verifications/confirmation {"email": "hyeja@example.com", "code": "384021"}
    @Operation(
            summary = "이메일 인증 코드 확인",
            description = "메일로 받은 코드를 확인합니다. 통과하면 30분 안에 이 이메일로 회원가입할 수 있습니다. "
                    + "5번 틀리면 코드를 다시 받아야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "형식 오류 (COMMON_003) / 코드 불일치 (VERIFY_001) / 만료됐거나 발송 이력 없음 (VERIFY_002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "5회 넘게 실패 (VERIFY_005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/confirmation")
    public ApiResponse<EmailVerificationResponseDTO.ConfirmDTO> confirm(
            @Valid @RequestBody EmailVerificationRequestDTO.ConfirmDTO request
    ) {
        emailVerificationService.confirm(request.getEmail(), request.getCode());
        return ApiResponse.onSuccess(new EmailVerificationResponseDTO.ConfirmDTO(true));
    }
}
