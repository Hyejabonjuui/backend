package com.hyeja.domain.member.converter;

import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.entity.Member;

// 회원 엔티티 → 응답 DTO 변환을 모아 둡니다. 서비스는 조회·검증만 맡고 변환은 여기서 합니다.
// (같은 converter 패키지명이지만 policy·profile의 JPA AttributeConverter와는 용도가 다릅니다.)
public class MemberConverter {

    // 마이페이지(S-08) 계정 탭 응답으로 변환합니다.
    public static MemberAccountResponseDTO toMemberAccountResponseDTO(Member member) {
        return MemberAccountResponseDTO.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
