package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 회원가입 이메일 인증 응답입니다.
public final class EmailVerificationResponseDTO {

    private EmailVerificationResponseDTO() {
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "EmailVerificationSendResponseDTO", description = "인증 코드 발송 응답")
    public static class SendDTO {

        @Schema(description = "인증 코드 유효 시간(초). 프론트 타이머에 사용합니다.", example = "300")
        private long expiresInSeconds;
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "EmailVerificationConfirmResponseDTO", description = "인증 코드 확인 응답")
    public static class ConfirmDTO {

        @Schema(description = "인증 성공 여부 (실패는 에러 코드로 응답하므로 항상 true)", example = "true")
        private boolean verified;
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "EmailVerificationMismatchResultDTO", description = "인증 코드 불일치(VERIFY_001) 에러 응답의 result")
    public static class MismatchDTO {

        @Schema(description = "남은 시도 횟수. 0이 되기 전에 맞히지 못하면 1시간 동안 인증이 막힙니다.", example = "4")
        private long remainingAttempts;
    }
}
