package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원 탈퇴 요청입니다. 본인 확인용으로 비밀번호를 다시 받습니다(S-08 탈퇴 확인 모달).
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberWithdrawRequestDTO", description = "회원 탈퇴 요청")
public class MemberWithdrawRequestDTO {

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Schema(description = "현재 비밀번호", example = "hyeja1234!")
    private String password;
}
