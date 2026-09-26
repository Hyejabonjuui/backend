package com.hyeja.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 요청입니다. 형식 검사(이메일 형식·비밀번호 규칙)는 하지 않습니다.
// 형식이 틀린 값은 어차피 가입된 계정과 맞지 않아 로그인 실패(MEMBER_005)로 처리됩니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberLoginRequestDTO", description = "로그인 요청")
public class MemberLoginRequestDTO {

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Schema(description = "이메일", example = "hyeja@example.com")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Schema(description = "비밀번호", example = "hyeja1234!")
    private String password;
}
