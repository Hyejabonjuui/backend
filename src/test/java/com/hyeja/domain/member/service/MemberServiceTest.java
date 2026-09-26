package com.hyeja.domain.member.service;

import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.dto.MemberAccountResponseDTO;
import com.hyeja.domain.member.dto.MemberFindEmailResponseDTO;
import com.hyeja.domain.member.dto.MemberSignupRequestDTO;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.domain.profile.dto.ProfileRequestDTO;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void returnsAccountOfExistingMember() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 22, 14, 3, 11);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1L, createdAt)));

        MemberAccountResponseDTO result = memberService.getMyAccount(1L);

        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("hyeja@example.com");
        assertThat(result.getNickname()).isEqualTo("민지");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void throwsMemberNotFoundWhenMemberDoesNotExist() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertMemberNotFound(99L);
    }

    @Test
    void throwsMemberNotFoundWhenMemberIsDeleted() {
        Member deleted = member(1L, LocalDateTime.now());
        deleted.softDelete();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertMemberNotFound(1L);
    }

    private void assertMemberNotFound(Long memberId) {
        assertThatThrownBy(() -> memberService.getMyAccount(memberId))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
    }

    @Test
    void signupSavesMemberWithEncodedPasswordAndProfileTogether() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 25, 14, 3, 11);
        Region region = Region.builder().regionCode("11440").sigunguName("서울특별시 마포구").build();
        when(memberRepository.existsByEmail("hyeja@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("민지")).thenReturn(false);
        when(regionRepository.findById("11440")).thenReturn(Optional.of(region));
        when(passwordEncoder.encode("hyeja1234!")).thenReturn("encoded-password");
        // 저장 시 DB가 채우는 ID·가입일을 흉내 냅니다.
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "memberId", 1L);
            ReflectionTestUtils.setField(saved, "createdAt", createdAt);
            return saved;
        });

        MemberAccountResponseDTO result = memberService.signup(signupRequest("11440"));

        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("hyeja@example.com");
        assertThat(result.getNickname()).isEqualTo("민지");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);

        // 비밀번호는 원문이 아니라 암호화한 값으로 저장됩니다.
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getPassword()).isEqualTo("encoded-password");

        // 조건은 방금 저장한 회원·확인한 지역과 함께 저장됩니다.
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        Profile profile = profileCaptor.getValue();
        assertThat(profile.getEmail()).isEqualTo("hyeja@example.com");
        assertThat(profile.getRegion()).isSameAs(region);
        assertThat(profile.getBirth()).isEqualTo(LocalDate.of(2000, 3, 15));
        assertThat(profile.getEmploymentCode()).isEqualTo(EmploymentStatus.EMPLOYED);
        assertThat(profile.getMarriageCode()).isNull();
    }

    @Test
    void signupThrowsWhenEmailIsDuplicated() {
        when(memberRepository.existsByEmail("hyeja@example.com")).thenReturn(true);

        assertSignupError("11440", ErrorStatus.MEMBER_EMAIL_DUPLICATED);
    }

    @Test
    void signupThrowsWhenNicknameIsDuplicated() {
        when(memberRepository.existsByEmail("hyeja@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("민지")).thenReturn(true);

        assertSignupError("11440", ErrorStatus.MEMBER_NICKNAME_DUPLICATED);
    }

    @Test
    void signupThrowsRegionNotFoundBeforeSavingMember() {
        when(memberRepository.existsByEmail("hyeja@example.com")).thenReturn(false);
        when(memberRepository.existsByNickname("민지")).thenReturn(false);
        when(regionRepository.findById("99999")).thenReturn(Optional.empty());

        assertSignupError("99999", ErrorStatus.REGION_NOT_FOUND);
    }

    // 실패하면 회원·조건 모두 저장하지 않아야 합니다.
    private void assertSignupError(String regionCode, ErrorStatus expected) {
        assertThatThrownBy(() -> memberService.signup(signupRequest(regionCode)))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(expected);
        verify(memberRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    // 조건의 선택 항목(혼인·소득·학력·주거 형태)은 비워서 보냅니다.
    private MemberSignupRequestDTO signupRequest(String regionCode) {
        return MemberSignupRequestDTO.builder()
                .email("hyeja@example.com")
                .password("hyeja1234!")
                .nickname("민지")
                .profile(ProfileRequestDTO.builder()
                        .birth(LocalDate.of(2000, 3, 15))
                        .regionCode(regionCode)
                        .employmentCode(EmploymentStatus.EMPLOYED)
                        .houselessYn(true)
                        .build())
                .build();
    }

    @Test
    void findEmailReturnsMaskedEmailAndJoinDate() {
        Member member = member(1L, LocalDateTime.of(2026, 9, 20, 14, 3, 11));
        when(memberRepository.findByNickname("민지")).thenReturn(Optional.of(member));
        when(profileRepository.findById("hyeja@example.com"))
                .thenReturn(Optional.of(profile(member, LocalDate.of(2000, 3, 15))));

        MemberFindEmailResponseDTO result = memberService.findEmail("민지", LocalDate.of(2000, 3, 15));

        assertThat(result.getEmail()).isEqualTo("hye***@example.com");
        assertThat(result.getJoinedAt()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void findEmailThrowsSameErrorWhenNicknameIsWrong() {
        when(memberRepository.findByNickname("없는닉네임")).thenReturn(Optional.empty());

        assertFindEmailFails("없는닉네임", LocalDate.of(2000, 3, 15));
    }

    @Test
    void findEmailThrowsSameErrorWhenBirthIsWrong() {
        Member member = member(1L, LocalDateTime.now());
        when(memberRepository.findByNickname("민지")).thenReturn(Optional.of(member));
        when(profileRepository.findById("hyeja@example.com"))
                .thenReturn(Optional.of(profile(member, LocalDate.of(2000, 3, 15))));

        assertFindEmailFails("민지", LocalDate.of(1999, 1, 1));
    }

    @Test
    void findEmailThrowsSameErrorWhenMemberIsDeleted() {
        Member deleted = member(1L, LocalDateTime.now());
        deleted.softDelete();
        when(memberRepository.findByNickname("민지")).thenReturn(Optional.of(deleted));

        assertFindEmailFails("민지", LocalDate.of(2000, 3, 15));
    }

    // 닉네임·생년월일·탈퇴 중 무엇이 원인이든 같은 에러여야 합니다 (가입된 닉네임 노출 방지).
    private void assertFindEmailFails(String nickname, LocalDate birth) {
        assertThatThrownBy(() -> memberService.findEmail(nickname, birth))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_EMAIL_NOT_FOUND);
    }

    @Test
    void withdrawSoftDeletesMemberAndProfileAndDeletesFavoritesAndNotifications() {
        Member member = member(1L, LocalDateTime.now());
        Profile profile = profile(member, LocalDate.of(2000, 3, 15));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(profileRepository.findById("hyeja@example.com")).thenReturn(Optional.of(profile));

        memberService.withdraw(1L);

        assertThat(member.isDeleted()).isTrue();
        assertThat(profile.isDeleted()).isTrue();
        verify(favoriteRepository).deleteByMemberMemberId(1L);
        verify(notificationRepository).deleteByMemberMemberId(1L);
    }

    // 이미 탈퇴한 회원은 404이고, 관심 정책·알림도 건드리지 않습니다.
    @Test
    void withdrawThrowsMemberNotFoundWhenAlreadyDeleted() {
        Member deleted = member(1L, LocalDateTime.now());
        deleted.softDelete();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> memberService.withdraw(1L))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verify(favoriteRepository, never()).deleteByMemberMemberId(anyLong());
        verify(notificationRepository, never()).deleteByMemberMemberId(anyLong());
    }

    private Profile profile(Member member, LocalDate birth) {
        return Profile.builder()
                .member(member)
                .region(Region.builder().regionCode("11440").sigunguName("서울특별시 마포구").build())
                .birth(birth)
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true)
                .build();
    }

    // ID·가입일은 DB와 JPA Auditing이 채우는 값이라 테스트에서는 직접 넣습니다.
    private Member member(Long memberId, LocalDateTime createdAt) {
        Member member = Member.builder()
                .email("hyeja@example.com")
                .password("encoded-password")
                .nickname("민지")
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "createdAt", createdAt);
        return member;
    }
}
