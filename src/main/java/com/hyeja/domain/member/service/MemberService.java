package com.hyeja.domain.member.service;

import com.hyeja.domain.member.converter.MemberConverter;
import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.dto.MemberSignupRequestDTO;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.profile.converter.ProfileConverter;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    // 회원가입 때 조건도 함께 저장합니다. ProfileService는 MemberService를 쓰므로(순환 참조 방지) 저장소를 직접 씁니다.
    private final ProfileRepository profileRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계정 정보와 내 조건을 한 트랜잭션에서 함께 저장합니다. 조건 없는 회원이 생기지 않도록,
     * 중간에 하나라도 실패하면 회원·조건 모두 저장되지 않습니다(롤백).
     * 이메일 중복이면 MEMBER_EMAIL_DUPLICATED(409), 닉네임 중복이면 MEMBER_NICKNAME_DUPLICATED(409),
     * 지역 코드가 REGION에 없으면 REGION_NOT_FOUND(404)를 던집니다. 탈퇴 회원의 이메일·닉네임도 중복으로 봅니다.
     */
    @Transactional
    public MemberAccountResponseDTO signup(MemberSignupRequestDTO request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(ErrorStatus.MEMBER_EMAIL_DUPLICATED);
        }
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new GeneralException(ErrorStatus.MEMBER_NICKNAME_DUPLICATED);
        }
        // 회원을 저장하기 전에 지역부터 확인해, 잘못된 지역 코드로 회원 행이 먼저 생기지 않게 합니다.
        Region region = regionRepository.findById(request.getProfile().getRegionCode())
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_FOUND));

        // 비밀번호는 원문을 저장하지 않고 BCrypt로 암호화한 값만 저장합니다.
        Member member = memberRepository.save(
                MemberConverter.toMember(request, passwordEncoder.encode(request.getPassword())));
        profileRepository.save(ProfileConverter.toProfile(member, region, request.getProfile()));

        return MemberConverter.toMemberAccountResponseDTO(member);
    }

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
