package com.hyeja.domain.member.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 마이페이지(S-08) 계정 탭에 보여줄 내 계정 정보 응답입니다.
// 비밀번호·권한은 화면에 필요 없고 노출되면 안 되므로 담지 않습니다.
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberAccountResponseDTO {

    private Long memberId;

    private String email;

    private String nickname;

    // 가입일 (BaseEntity의 created_at)
    private LocalDateTime createdAt;
}
