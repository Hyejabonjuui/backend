package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이메일 찾기 응답입니다. 명세대로 목록(emails)으로 내려주며, 닉네임이 중복 불가라 항목은 1건입니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberFindEmailResponseDTO", description = "이메일 찾기 응답")
public class MemberFindEmailResponseDTO {

    @Schema(description = "찾은 이메일 목록")
    private List<FoundEmailDTO> emails;

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "FoundEmailDTO", description = "찾은 이메일")
    public static class FoundEmailDTO {

        @Schema(description = "가린 이메일 (@ 앞 3글자만 표시, 3글자 이하면 첫 글자만)", example = "min***@hyeja.kr")
        private String email;

        @Schema(description = "가입일", example = "2026-09-20")
        private LocalDate joinedAt;
    }
}
