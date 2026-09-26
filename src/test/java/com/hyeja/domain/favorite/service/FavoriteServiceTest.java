package com.hyeja.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteItemDTO;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void returnsConvertedFavoritePolicies() {
        Member member = member();
        Favorite favorite = favorite(10L, member);
        when(memberService.getActiveMember(1L)).thenReturn(member);
        when(favoriteRepository.findAllActiveByMemberId(1L, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(favorite), PageRequest.of(0, 8), 9));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, null, 0, 8);

        assertThat(result.getFavorites()).singleElement().satisfies(item -> {
            assertThat(item.getFavoriteId()).isEqualTo(10L);
            assertThat(item.getPolicyId()).isEqualTo("policy-1");
            assertThat(item.getPolicyName()).isEqualTo("청년 월세 지원");
            assertThat(item.getCategoryCodes()).containsExactly(PolicyCategory.MONTHLY_RENT);
            assertThat(item.getCategoryNames()).containsExactly("월세");
            assertThat(item.getSupportContent()).isEqualTo("월세를 지원합니다.");
            assertThat(item.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(item.getApplyPeriodCode()).isEqualTo(
                    com.hyeja.domain.policy.enums.PolicyApplyPeriod.CLOSED);
            assertThat(item.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 30));
        });
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(8);
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
        verify(memberService).getActiveMember(1L);
    }

    @Test
    void returnsEmptyListWhenMemberHasNoFavorites() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(favoriteRepository.findAllActiveByMemberId(1L, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, null, 0, 8);

        assertThat(result.getFavorites()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void searchesFavoritesWithTrimmedKeyword() {
        Favorite favorite = favorite(10L, member());
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                1L, "월세", PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(favorite), PageRequest.of(0, 8), 1));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, "  월세  ", 0, 8);

        assertThat(result.getFavorites()).singleElement()
                .extracting(FavoriteItemDTO::getPolicyName)
                .isEqualTo("청년 월세 지원");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(favoriteRepository).searchAllActiveByMemberIdAndKeyword(
                1L, "월세", PageRequest.of(0, 8));
    }

    @Test
    void escapesLikeWildcardsAndEscapeCharacterInKeyword() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                1L, "50!%!_할인!!", PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        favoriteService.getMyFavorites(1L, "  50%_할인!  ", 0, 8);

        verify(favoriteRepository).searchAllActiveByMemberIdAndKeyword(
                1L, "50!%!_할인!!", PageRequest.of(0, 8));
    }

    @Test
    void treatsBlankKeywordAsUnfilteredList() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(favoriteRepository.findAllActiveByMemberId(1L, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, "   ", 0, 8);

        assertThat(result.getFavorites()).isEmpty();
        verify(favoriteRepository).findAllActiveByMemberId(1L, PageRequest.of(0, 8));
        verify(favoriteRepository, never()).searchAllActiveByMemberIdAndKeyword(
                any(), any(), any());
    }

    @Test
    void returnsEmptyListWhenSearchHasNoMatches() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(favoriteRepository.searchAllActiveByMemberIdAndKeyword(
                1L, "없는 정책", PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, "없는 정책", 0, 8);

        assertThat(result.getFavorites()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    void throwsMemberNotFoundWithoutQueryingFavorites() {
        when(memberService.getActiveMember(99L))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> favoriteService.getMyFavorites(99L, null, 0, 8))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    void createsAndReturnsFavoritePolicy() {
        Member member = member();
        Policy policy = policy();
        Favorite saved = favorite(10L, member, policy);
        when(memberService.getActiveMember(1L)).thenReturn(member);
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy));
        when(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(1L, "policy-1"))
                .thenReturn(false);
        when(favoriteRepository.saveAndFlush(any(Favorite.class))).thenReturn(saved);

        FavoriteItemDTO result = favoriteService.createFavorite(1L, "policy-1");

        assertThat(result.getFavoriteId()).isEqualTo(10L);
        assertThat(result.getPolicyId()).isEqualTo("policy-1");
        assertThat(result.getPolicyName()).isEqualTo("청년 월세 지원");
        assertThat(result.getCategoryCodes()).containsExactly(PolicyCategory.MONTHLY_RENT);
        assertThat(result.getCategoryNames()).containsExactly("월세");
        assertThat(result.getApplyPeriodCode()).isEqualTo(
                com.hyeja.domain.policy.enums.PolicyApplyPeriod.CLOSED);
        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 24, 10, 30));
        verify(favoriteRepository).saveAndFlush(any(Favorite.class));
    }

    @Test
    void createThrowsMemberNotFoundBeforePolicyLookup() {
        when(memberService.getActiveMember(99L))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertCreateError(99L, "policy-1", ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(policyRepository, favoriteRepository);
    }

    @Test
    void createThrowsPolicyNotFoundWhenPolicyDoesNotExist() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.findById("missing-policy")).thenReturn(Optional.empty());

        assertCreateError(1L, "missing-policy", ErrorStatus.POLICY_NOT_FOUND);
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    void createThrowsPolicyNotFoundWhenPolicyIsInactive() {
        Policy inactivePolicy = policy();
        ReflectionTestUtils.setField(inactivePolicy, "activeYn", false);
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(inactivePolicy));

        assertCreateError(1L, "policy-1", ErrorStatus.POLICY_NOT_FOUND);
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    void createThrowsConflictWhenFavoriteAlreadyExists() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy()));
        when(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(1L, "policy-1"))
                .thenReturn(true);

        assertCreateError(1L, "policy-1", ErrorStatus.FAVORITE_ALREADY_EXISTS);
    }

    @Test
    void createConvertsDatabaseUniqueViolationToConflict() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.findById("policy-1")).thenReturn(Optional.of(policy()));
        when(favoriteRepository.existsByMemberMemberIdAndPolicyPolicyId(1L, "policy-1"))
                .thenReturn(false);
        when(favoriteRepository.saveAndFlush(any(Favorite.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate favorite"));

        assertCreateError(1L, "policy-1", ErrorStatus.FAVORITE_ALREADY_EXISTS);
    }

    @Test
    void hardDeletesFavoritePolicy() {
        Member member = member();
        Favorite favorite = favorite(10L, member);
        when(memberService.getActiveMember(1L)).thenReturn(member);
        when(policyRepository.existsById("policy-1")).thenReturn(true);
        when(favoriteRepository.findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                1L, "policy-1")).thenReturn(Optional.of(favorite));

        favoriteService.deleteFavorite(1L, "policy-1");

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void deleteThrowsMemberNotFoundBeforePolicyLookup() {
        when(memberService.getActiveMember(99L))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertDeleteError(99L, "policy-1", ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(policyRepository, favoriteRepository);
    }

    @Test
    void deleteThrowsPolicyNotFoundWhenPolicyDoesNotExist() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.existsById("missing-policy")).thenReturn(false);

        assertDeleteError(1L, "missing-policy", ErrorStatus.POLICY_NOT_FOUND);
        verifyNoInteractions(favoriteRepository);
    }

    @Test
    void deleteThrowsFavoriteNotFoundWhenFavoriteDoesNotExist() {
        when(memberService.getActiveMember(1L)).thenReturn(member());
        when(policyRepository.existsById("policy-1")).thenReturn(true);
        when(favoriteRepository.findByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                1L, "policy-1")).thenReturn(Optional.empty());

        assertDeleteError(1L, "policy-1", ErrorStatus.FAVORITE_NOT_FOUND);
    }

    private void assertCreateError(Long memberId, String policyId, ErrorStatus expected) {
        assertThatThrownBy(() -> favoriteService.createFavorite(memberId, policyId))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(expected);
    }

    private void assertDeleteError(Long memberId, String policyId, ErrorStatus expected) {
        assertThatThrownBy(() -> favoriteService.deleteFavorite(memberId, policyId))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(expected);
    }

    private Member member() {
        return Member.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("회원")
                .build();
    }

    private Favorite favorite(Long favoriteId, Member member) {
        return favorite(favoriteId, member, policy());
    }

    private Policy policy() {
        return Policy.builder()
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .categories(java.util.Set.of(PolicyCategory.MONTHLY_RENT))
                .supportContent("월세를 지원합니다.")
                .ageLimitYn(false)
                .applyPeriodCode(com.hyeja.domain.policy.enums.PolicyApplyPeriod.CLOSED)
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .build();
    }

    private Favorite favorite(Long favoriteId, Member member, Policy policy) {
        Favorite favorite = Favorite.builder()
                .member(member)
                .policy(policy)
                .build();
        ReflectionTestUtils.setField(favorite, "favoriteId", favoriteId);
        ReflectionTestUtils.setField(favorite, "createdAt", LocalDateTime.of(2026, 9, 24, 10, 30));
        return favorite;
    }
}
