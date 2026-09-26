package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이메일 찾기 응답입니다. 닉네임은 중복 불가라 결과가 항상 1건이고, 못 찾으면 MEMBER_004 에러라 목록이 아닌 단일 객체로 내려줍니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberFindEmailResponseDTO", description = "이메일 찾기 응답")
public class MemberFindEmailResponseDTO {

    @Schema(description = "가린 이메일 (@ 앞 3글자만 표시, 3글자 이하면 첫 글자만)", example = "min***@hyeja.kr")
    private String email;

    @Schema(description = "가입일", example = "2026-09-20")
    private LocalDate joinedAt;
}
