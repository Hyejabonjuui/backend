package com.hyeja.domain.member.dto;

import com.hyeja.domain.profile.dto.ProfileRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입 요청입니다. 계정 정보(S-03)와 내 조건(S-04)을 한 번에 받습니다.
// 프론트는 S-03 입력값을 들고 있다가 S-04 완료 시 함께 보냅니다. 조건 없이는 가입할 수 없습니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "MemberSignupRequestDTO", description = "회원가입 요청 (계정 정보 + 내 조건)")
public class MemberSignupRequestDTO {

    @Schema(description = "이메일 (로그인 ID)", example = "hyeja@example.com")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    // 영문·숫자·특수문자를 각각 1자 이상 포함한 8~20자. 특수문자는 \p{Punct}(!@#$%^&* 등 키보드 기호)입니다.
    @Schema(description = "비밀번호 (8~20자, 영문+숫자+특수문자 포함)", example = "hyeja1234!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*\\p{Punct}).{8,20}$",
            message = "비밀번호는 영문·숫자·특수문자를 각각 1자 이상 포함한 8~20자여야 합니다.")
    private String password;

    @Schema(description = "닉네임 (최대 20자)", example = "민지")
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
    private String nickname;

    // @Valid가 있어야 profile 안쪽 필드(생년월일·거주지 등)까지 검증합니다.
    @Schema(description = "내 조건")
    @NotNull(message = "내 조건은 필수입니다.")
    @Valid
    private ProfileRequestDTO profile;
}
