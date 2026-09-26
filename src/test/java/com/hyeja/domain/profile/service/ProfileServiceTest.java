package com.hyeja.domain.profile.service;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.profile.dto.ProfileRequestDTO;
import com.hyeja.domain.profile.dto.ProfileResponseDTO;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.enums.IncomeRange;
import com.hyeja.domain.profile.enums.MaritalStatus;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String EMAIL = "hyeja@example.com";

    @Mock
    private MemberService memberService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void returnsProfileWithCodeNamesAndAge() {
        // 올해 생일이 내일이라 아직 26세가 되지 않은 경우 → 만 25세
        LocalDate birth = LocalDate.now().minusYears(26).plusDays(1);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 22, 14, 10, 2);
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.of(profile(birth, updatedAt)));

        ProfileResponseDTO result = profileService.getMyProfile(1L);

        assertThat(result.getBirth()).isEqualTo(birth);
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getRegionCode()).isEqualTo("11440");
        assertThat(result.getRegionName()).isEqualTo("서울특별시 마포구");
        assertThat(result.getEmploymentCode()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(result.getEmploymentName()).isEqualTo("재직자");
        assertThat(result.getHouselessYn()).isTrue();
        assertThat(result.getMarriageCode()).isEqualTo(MaritalStatus.SINGLE);
        assertThat(result.getMarriageName()).isEqualTo("미혼");
        assertThat(result.getHousingType()).isEqualTo(HousingType.MONTHLY_RENT);
        assertThat(result.getHousingTypeName()).isEqualTo("월세");
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
        // 등록하지 않은 선택 항목은 코드·이름 모두 null
        assertThat(result.getIncomeRangeCode()).isNull();
        assertThat(result.getIncomeRangeName()).isNull();
        assertThat(result.getEducationCode()).isNull();
        assertThat(result.getEducationName()).isNull();
    }

    @Test
    void throwsMemberNotFoundWhenMemberIsMissingOrDeleted() {
        when(memberService.getActiveMember(99L)).thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertError(99L, ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(profileRepository);
    }

    @Test
    void throwsProfileNotFoundWhenProfileIsNotRegistered() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.empty());

        assertError(1L, ErrorStatus.PROFILE_NOT_FOUND);
    }

    @Test
    void throwsProfileNotFoundWhenProfileIsDeleted() {
        Profile deleted = profile(LocalDate.of(2000, 3, 15), LocalDateTime.now());
        deleted.softDelete();
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.of(deleted));

        assertError(1L, ErrorStatus.PROFILE_NOT_FOUND);
    }

    @Test
    void updatesAllFieldsAndClearsOptionalFieldsSentAsNull() {
        Profile profile = profile(LocalDate.of(2000, 3, 15), LocalDateTime.now());
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.of(profile));
        when(regionRepository.findById("41111"))
                .thenReturn(Optional.of(Region.builder().regionCode("41111").sigunguName("경기도 수원시 장안구").build()));

        // 기존: 마포구·재직자·무주택·미혼·월세 → 수원시 장안구·미취업자·유주택·소득 입력, 혼인·주거 형태는 비움
        ProfileResponseDTO result = profileService.updateMyProfile(1L, ProfileRequestDTO.builder()
                .birth(LocalDate.of(1999, 1, 1))
                .regionCode("41111")
                .employmentCode(EmploymentStatus.UNEMPLOYED)
                .houselessYn(false)
                .incomeRangeCode(IncomeRange.R3000_4000)
                .build());

        assertThat(result.getBirth()).isEqualTo(LocalDate.of(1999, 1, 1));
        assertThat(result.getRegionCode()).isEqualTo("41111");
        assertThat(result.getRegionName()).isEqualTo("경기도 수원시 장안구");
        assertThat(result.getEmploymentCode()).isEqualTo(EmploymentStatus.UNEMPLOYED);
        assertThat(result.getHouselessYn()).isFalse();
        assertThat(result.getIncomeRangeCode()).isEqualTo(IncomeRange.R3000_4000);
        // null로 보낸 선택 항목은 기존 값(미혼·월세)이 지워집니다.
        assertThat(result.getMarriageCode()).isNull();
        assertThat(result.getHousingType()).isNull();
        // 수정 시각이 응답에 담기도록 응답 전에 DB에 반영합니다.
        verify(profileRepository).flush();
    }

    @Test
    void updateThrowsMemberNotFoundWhenMemberIsMissingOrDeleted() {
        when(memberService.getActiveMember(99L)).thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertUpdateError(99L, "11440", ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(profileRepository, regionRepository);
    }

    @Test
    void updateThrowsProfileNotFoundWhenProfileIsMissing() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.empty());

        assertUpdateError(1L, "11440", ErrorStatus.PROFILE_NOT_FOUND);
        verifyNoInteractions(regionRepository);
    }

    @Test
    void updateThrowsRegionNotFoundAndKeepsProfile() {
        Profile profile = profile(LocalDate.of(2000, 3, 15), LocalDateTime.now());
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(profileRepository.findById(EMAIL)).thenReturn(Optional.of(profile));
        when(regionRepository.findById("99999")).thenReturn(Optional.empty());

        assertUpdateError(1L, "99999", ErrorStatus.REGION_NOT_FOUND);
        // 실패하면 기존 조건이 그대로 남습니다.
        assertThat(profile.getRegion().getRegionCode()).isEqualTo("11440");
        assertThat(profile.getBirth()).isEqualTo(LocalDate.of(2000, 3, 15));
        verify(profileRepository, never()).flush();
    }

    private void assertUpdateError(Long memberId, String regionCode, ErrorStatus expected) {
        ProfileRequestDTO request = ProfileRequestDTO.builder()
                .birth(LocalDate.of(1999, 1, 1))
                .regionCode(regionCode)
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true)
                .build();
        assertThatThrownBy(() -> profileService.updateMyProfile(memberId, request))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(expected);
    }

    private void assertError(Long memberId, ErrorStatus expected) {
        assertThatThrownBy(() -> profileService.getMyProfile(memberId))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(expected);
    }

    private Member member() {
        return Member.builder()
                .email(EMAIL)
                .password("encoded-password")
                .nickname("민지")
                .build();
    }

    // 소득·학력은 등록하지 않은 상태로 만듭니다. 수정 시각은 JPA Auditing 값이라 직접 넣습니다.
    private Profile profile(LocalDate birth, LocalDateTime updatedAt) {
        Profile profile = Profile.builder()
                .member(member())
                .region(Region.builder().regionCode("11440").sigunguName("서울특별시 마포구").build())
                .birth(birth)
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true)
                .marriageCode(MaritalStatus.SINGLE)
                .housingType(HousingType.MONTHLY_RENT)
                .build();
        ReflectionTestUtils.setField(profile, "updatedAt", updatedAt);
        return profile;
    }
}
