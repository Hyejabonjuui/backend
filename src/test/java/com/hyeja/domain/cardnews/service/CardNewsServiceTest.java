package com.hyeja.domain.cardnews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyApplyPeriod;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyHouselessRequirement;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardNewsServiceTest {
    private final CardNewsRepository cardNewsRepository = mock(CardNewsRepository.class);
    private final PolicyRegionRepository policyRegionRepository = mock(PolicyRegionRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final CardNewsService service = new CardNewsService(
            cardNewsRepository, policyRegionRepository, favoriteRepository,
            memberRepository, profileRepository);

    @Test
    void returnsPopupDataForAuthenticatedMember() {
        Policy policy = policy();
        List<CardNews> cards = List.of(
                card(policy, 1L, "정책명", "정책 소개"),
                card(policy, 2L, null, "신청 대상"),
                card(policy, 3L, "지원 혜택", "혜택 설명"),
                card(policy, 4L, "2026-09-01 ~ 2026-09-30", "신청 방법"));
        Region region = Region.builder()
                .regionCode("11000").sigunguName("서울특별시").build();
        when(cardNewsRepository.findAllActiveByPolicyIdOrderByCardNo("policy-1"))
                .thenReturn(cards);
        when(policyRegionRepository.findAllActiveByPolicyIds(List.of("policy-1")))
                .thenReturn(List.of(PolicyRegion.builder().policy(policy).region(region).build()));
        when(favoriteRepository
                .existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(7L, "policy-1"))
                .thenReturn(true);

        var response = service.getCardNewsDetail("policy-1", 7L);

        assertThat(response.policyId()).isEqualTo("policy-1");
        assertThat(response.categoryLabel()).isEqualTo("월세");
        assertThat(response.dDay()).isEqualTo(5);
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.isFavorite()).isTrue();
        assertThat(response.applyUrl()).isEqualTo("https://example.com/apply");
        assertThat(response.cards()).extracting(card -> card.cardNo())
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(response.cards().get(1).badges())
                .containsExactly("만 19~34세", "서울특별시");
        assertThat(response.cards().get(0).badges()).isEmpty();
    }

    @Test
    void treatsMissingPrincipalAsGuest() {
        Policy policy = policy();
        CardNews card = card(policy, 1L, "정책명", "정책 소개");
        when(cardNewsRepository.findAllActiveByPolicyIdOrderByCardNo("policy-1"))
                .thenReturn(List.of(card));
        when(policyRegionRepository.findAllActiveByPolicyIds(List.of("policy-1")))
                .thenReturn(List.of());

        var response = service.getCardNewsDetail("policy-1", null);

        assertThat(response.isAuthenticated()).isFalse();
        assertThat(response.isFavorite()).isFalse();
        verify(favoriteRepository, never())
                .existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsUnknownCardNews() {
        when(cardNewsRepository.findAllActiveByPolicyIdOrderByCardNo("missing-policy"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getCardNewsDetail("missing-policy", null))
                .isInstanceOfSatisfying(GeneralException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus.CARD_NEWS_NOT_FOUND));
    }

    @Test
    void usesPolicyDescriptionInsteadOfCardBodyForHomeCards() {
        Policy policy = policy();
        CardNews card = card(policy, 1L, "정책명", "기호가 포함된 원문 | / <> ");
        when(cardNewsRepository.findGuestHomeCardNews(any(LocalDate.class), any()))
                .thenReturn(List.of(card));

        var response = service.getGuestCardNews();

        assertThat(response).singleElement()
                .extracting(item -> item.getDescription())
                .isEqualTo("정제된 정책 설명");
    }

    private Policy policy() {
        return Policy.builder()
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .description("정제된 정책 설명")
                .categories(Set.of(PolicyCategory.MONTHLY_RENT))
                .minAge(19)
                .maxAge(34)
                .ageLimitYn(true)
                .houselessRequirement(PolicyHouselessRequirement.UNKNOWN)
                .applyPeriodCode(PolicyApplyPeriod.SPECIFIC_PERIOD)
                .applyStartDate(LocalDate.now().minusDays(5))
                .applyEndDate(LocalDate.now().plusDays(5))
                .applyUrl("https://example.com/apply")
                .build();
    }

    private CardNews card(Policy policy, Long cardNo, String title, String body) {
        return CardNews.builder()
                .policy(policy)
                .cardNo(cardNo)
                .title(title)
                .body(body)
                .build();
    }
}
