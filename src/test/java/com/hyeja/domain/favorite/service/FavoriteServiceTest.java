package com.hyeja.domain.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.entity.Favorite;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.service.MemberService;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void returnsConvertedFavoritePolicies() {
        Member member = member();
        Favorite favorite = favorite(10L, member);
        when(memberService.getActiveMember(1L)).thenReturn(member);
        when(favoriteRepository.findAllActiveByMemberId(1L, PageRequest.of(0, 8)))
                .thenReturn(new PageImpl<>(List.of(favorite), PageRequest.of(0, 8), 9));

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, 0, 8);

        assertThat(result.getFavorites()).singleElement().satisfies(item -> {
            assertThat(item.getFavoriteId()).isEqualTo(10L);
            assertThat(item.getPolicyId()).isEqualTo("policy-1");
            assertThat(item.getPolicyName()).isEqualTo("청년 월세 지원");
            assertThat(item.getCategoryCode()).isEqualTo(PolicyCategory.MONTHLY_RENT);
            assertThat(item.getCategoryName()).isEqualTo("월세");
            assertThat(item.getSupportContent()).isEqualTo("월세를 지원합니다.");
            assertThat(item.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
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

        FavoriteListDTO result = favoriteService.getMyFavorites(1L, 0, 8);

        assertThat(result.getFavorites()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void throwsMemberNotFoundWithoutQueryingFavorites() {
        when(memberService.getActiveMember(99L))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> favoriteService.getMyFavorites(99L, 0, 8))
                .isInstanceOf(GeneralException.class)
                .extracting("code")
                .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
        verifyNoInteractions(favoriteRepository);
    }

    private Member member() {
        return Member.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("회원")
                .build();
    }

    private Favorite favorite(Long favoriteId, Member member) {
        Policy policy = Policy.builder()
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .category(PolicyCategory.MONTHLY_RENT)
                .supportContent("월세를 지원합니다.")
                .ageLimitYn(false)
                .applyPeriodCode("PERIOD")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .build();
        Favorite favorite = Favorite.builder()
                .member(member)
                .policy(policy)
                .build();
        ReflectionTestUtils.setField(favorite, "favoriteId", favoriteId);
        ReflectionTestUtils.setField(favorite, "createdAt", LocalDateTime.of(2026, 9, 24, 10, 30));
        return favorite;
    }
}
