package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 응답입니다. 프론트는 accessToken을 저장해 두고 요청 헤더에 Authorization: Bearer <accessToken>으로 보냅니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberLoginResponseDTO", description = "로그인 응답")
public class MemberLoginResponseDTO {

    @Schema(description = "액세스 토큰 (30분 유효)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "닉네임", example = "민지")
    private String nickname;
}
