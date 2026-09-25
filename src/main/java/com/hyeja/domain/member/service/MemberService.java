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
        return MemberConverter.toMemberAccountResponseDTO(getActiveMember(memberId));
    }

    /**
     * 탈퇴하지 않은 회원 엔티티를 조회합니다. 프로필 등 다른 도메인도 회원 확인에 이 메서드를 씁니다.
     * 회원이 없거나 탈퇴한 회원(deleted_at 기록)이면 MEMBER_NOT_FOUND(404)를 던집니다.
     *
     * 2026-09-25 수정 (#40 내 조건 조회): 원래 getMyAccount 안에 있던 회원 조회·탈퇴 확인 로직을
     * 이 메서드로 분리했습니다. 내 조건 조회(ProfileService)도 같은 확인이 필요해, 같은 코드를
     * 두 곳에 두지 않고 한 곳에서 관리하기 위해서입니다. getMyAccount의 동작은 바뀌지 않았습니다.
     */
    public Member getActiveMember(Long memberId) {
        // 탈퇴 회원도 행은 남아 있으므로(soft delete) 조회 후 isDeleted()로 걸러냅니다.
        return memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }
}
