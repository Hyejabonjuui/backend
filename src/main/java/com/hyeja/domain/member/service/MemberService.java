package com.hyeja.domain.member.service;

import com.hyeja.domain.member.converter.MemberConverter;
import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 회원 ID로 내 계정 정보(닉네임·이메일·가입일)를 조회합니다.
     * 인증 기반이 생기면 컨트롤러가 토큰에서 꺼낸 회원 ID를 넘겨 호출합니다.
     * 회원이 없거나 탈퇴한 회원(deleted_at 기록)이면 MEMBER_NOT_FOUND(404)를 던집니다.
     */
    public MemberAccountResponseDTO getMyAccount(Long memberId) {
        // 탈퇴 회원도 행은 남아 있으므로(soft delete) 조회 후 isDeleted()로 걸러냅니다.
        Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        return MemberConverter.toMemberAccountResponseDTO(member);
    }
}
