package com.hyeja.domain.cardnews.service;

import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO;
import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO.CardDTO;
import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.dto.MemberCardNewsResponseDTO;
import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardNewsService {

    private final CardNewsRepository cardNewsRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;

    public List<CardNewsResponseDTO> getGuestCardNews() {
        // Pageable 없이 레포지토리에서 상위 4개를 바로 조회
        List<CardNews> cardNewsList = cardNewsRepository.findGuestHomeCardNews(
                LocalDate.now(), PageRequest.of(0, 4));

        return cardNewsList.stream()
                .map(cn -> CardNewsResponseDTO.builder()
                        .policyId(cn.getPolicy().getPolicyId())
                        .policyName(cn.getPolicy().getPolicyName())
                        .description(cn.getPolicy().getDescription())
                        .applyEndDate(cn.getPolicy().getApplyEndDate() != null ? cn.getPolicy().getApplyEndDate().toString() : null)
                        .build()
                )
                .collect(Collectors.toList());
    }

    public List<MemberCardNewsResponseDTO> getMemberCardNews(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Profile profile = profileRepository.findById(member.getEmail())
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROFILE_NOT_FOUND));
        String regionCode = profile.getRegion().getRegionCode();

        return cardNewsRepository.findMemberHomeCardNews(
                        regionCode, regionCode.substring(0, 2) + "000",
                        LocalDate.now(), PageRequest.of(0, 4)).stream()
                .map(cardNews -> new MemberCardNewsResponseDTO(
                        cardNews.getPolicy().getPolicyId(),
                        cardNews.getPolicy().getPolicyName(),
                        cardNews.getPolicy().getDescription(),
                        cardNews.getPolicy().getApplyEndDate()))
                .toList();
    }

    public CardNewsDetailResponseDTO getCardNewsDetail(String policyId, Long memberId) {
        List<CardNews> cardNews = cardNewsRepository
                .findAllActiveByPolicyIdOrderByCardNo(policyId);
        if (cardNews.isEmpty()) {
            throw new GeneralException(ErrorStatus.CARD_NEWS_NOT_FOUND);
        }
        Policy policy = cardNews.get(0).getPolicy();
        List<PolicyRegion> policyRegions = policyRegionRepository
                .findAllActiveByPolicyIds(List.of(policy.getPolicyId()));
        List<String> eligibilityBadges = List.of(
                formatAge(policy), formatFirstRegion(policyRegions));
        boolean authenticated = memberId != null;

        List<CardDTO> cards = cardNews.stream()
                .map(card -> new CardDTO(
                        card.getCardNewsId(),
                        card.getCardNo(),
                        card.getTitle(),
                        card.getCardNo() == 2L ? eligibilityBadges : List.of(),
                        card.getBody()))
                .toList();

        return new CardNewsDetailResponseDTO(
                policy.getPolicyId(),
                policy.getCategories().stream().sorted().findFirst()
                        .map(category -> category.getLabel()).orElse(null),
                calculateDDay(policy),
                authenticated,
                authenticated && favoriteRepository
                        .existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                                memberId, policy.getPolicyId()),
                policy.getApplyUrl(),
                cards);
    }

    private String formatAge(Policy policy) {
        if (!Boolean.TRUE.equals(policy.getAgeLimitYn())) return "연령 제한 없음";
        if (policy.getMinAge() != null && policy.getMaxAge() != null) {
            return "만 %d~%d세".formatted(policy.getMinAge(), policy.getMaxAge());
        }
        if (policy.getMinAge() != null) return "만 %d세 이상".formatted(policy.getMinAge());
        if (policy.getMaxAge() != null) return "만 %d세 이하".formatted(policy.getMaxAge());
        return "연령 제한 없음";
    }

    private String formatFirstRegion(List<PolicyRegion> policyRegions) {
        return policyRegions.stream()
                .findFirst()
                .map(PolicyRegion::getRegion)
                .map(region -> region.getSigunguName())
                .orElse("전국");
    }

    private Integer calculateDDay(Policy policy) {
        return policy.getApplyEndDate() == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(
                        LocalDate.now(), policy.getApplyEndDate()));
    }
}
