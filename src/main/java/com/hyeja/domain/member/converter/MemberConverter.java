package com.hyeja.domain.member.converter;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.dto.MemberFindEmailResponseDTO;
import com.hyeja.domain.member.dto.MemberSignupRequestDTO;
import com.hyeja.domain.member.entity.Member;

// 회원 엔티티 ↔ DTO 변환을 모아 둡니다. 서비스는 조회·검증만 맡고 변환은 여기서 합니다.
// (같은 converter 패키지명이지만 policy·profile의 JPA AttributeConverter와는 용도가 다릅니다.)
public class MemberConverter {

    // 회원가입 요청으로 엔티티를 만듭니다. 비밀번호는 서비스에서 암호화한 값을 넘깁니다.
    public static Member toMember(MemberSignupRequestDTO request, String encodedPassword) {
        return Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .build();
    }

    // 마이페이지(S-08) 계정 탭 응답으로 변환합니다.
    public static MemberAccountResponseDTO toMemberAccountResponseDTO(Member member) {
        return MemberAccountResponseDTO.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .createdAt(member.getCreatedAt())
                .build();
    }

    // 이메일 찾기 응답으로 변환합니다. 이메일은 가리고, 가입일은 날짜만 내려줍니다.
    public static MemberFindEmailResponseDTO toFindEmailResponseDTO(Member member) {
        return MemberFindEmailResponseDTO.builder()
                .email(maskEmail(member.getEmail()))
                .joinedAt(member.getCreatedAt().toLocalDate())
                .build();
    }

    // @ 앞(로컬파트)은 앞 3글자만 남기고 ***로 가립니다. 3글자 이하면 첫 글자만 남깁니다. 도메인은 그대로 둡니다.
    // 예: mingy@hyeja.kr → min***@hyeja.kr, ab@hyeja.kr → a***@hyeja.kr
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        int visible = at > 3 ? 3 : 1;
        return email.substring(0, visible) + "***" + email.substring(at);
    }
}
