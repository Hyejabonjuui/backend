package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입 이메일 인증 요청입니다. 발송(SendDTO) → 확인(ConfirmDTO) 순서로 호출합니다.
public final class EmailVerificationRequestDTO {

    private EmailVerificationRequestDTO() {
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "EmailVerificationSendRequestDTO", description = "인증 코드 발송 요청")
    public static class SendDTO {

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아니에요.")
        @Schema(description = "인증 코드를 받을 이메일", example = "hyeja@example.com")
        private String email;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "EmailVerificationConfirmRequestDTO", description = "인증 코드 확인 요청")
    public static class ConfirmDTO {

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아니에요.")
        @Schema(description = "인증 코드를 받은 이메일", example = "hyeja@example.com")
        private String email;

        @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 숫자 6자리예요.")
        @Schema(description = "메일로 받은 6자리 숫자", example = "384021")
        private String code;
    }
}
