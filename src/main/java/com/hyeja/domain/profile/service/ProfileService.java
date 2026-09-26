package com.hyeja.domain.profile.service;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.profile.converter.ProfileConverter;
import com.hyeja.domain.profile.dto.ProfileRequestDTO;
import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final MemberService memberService;
    private final ProfileRepository profileRepository;
    private final RegionRepository regionRepository;

    /**
     * 회원 ID로 내 조건(생년월일·거주지·취업 상태 등)을 조회합니다.
     * 인증 기반이 생기면 컨트롤러가 토큰에서 꺼낸 회원 ID를 넘겨 호출합니다.
     * 회원이 없거나 탈퇴했으면 MEMBER_NOT_FOUND(404), 조건을 등록하지 않았으면 PROFILE_NOT_FOUND(404)를 던집니다.
     * 프론트는 PROFILE_NOT_FOUND를 받으면 온보딩(S-04)으로 이동합니다.
     */
    public ProfileResponseDTO getMyProfile(Long memberId) {
        return ProfileConverter.toProfileResponseDTO(getActiveProfile(memberId));
    }

    /**
     * 회원 ID로 내 조건을 전체 교체하고, 바뀐 조건을 조회 응답과 같은 형식으로 돌려줍니다.
     * 회원이 없거나 탈퇴했으면 MEMBER_NOT_FOUND(404), 조건이 없으면 PROFILE_NOT_FOUND(404),
     * 지역 코드가 REGION에 없으면 REGION_NOT_FOUND(404)를 던집니다.
     */
    @Transactional
    public ProfileResponseDTO updateMyProfile(Long memberId, ProfileRequestDTO request) {
        Profile profile = getActiveProfile(memberId);
        Region region = regionRepository.findById(request.getRegionCode())
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_FOUND));

        profile.update(region, request.getBirth(), request.getEmploymentCode(), request.getHouselessYn(),
                request.getMarriageCode(), request.getIncomeRangeCode(), request.getEducationCode(),
                request.getHousingType());
        // 수정 시각(updated_at)은 DB에 반영(flush)될 때 채워지므로, 응답에 새 시각이 담기도록 먼저 반영합니다.
        profileRepository.flush();

        return ProfileConverter.toProfileResponseDTO(profile);
    }

    // 탈퇴하지 않은 회원의, 삭제되지 않은 조건을 조회합니다. 조건 조회·수정과 정책 필터가 함께 씁니다.
    public Profile getActiveProfile(Long memberId) {
        Member member = memberService.getActiveMember(memberId);

        // 프로필 PK는 회원 이메일입니다. 삭제 기록(deleted_at)이 있는 프로필도 미등록으로 봅니다.
        return profileRepository.findById(member.getEmail())
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROFILE_NOT_FOUND));
    }
}
