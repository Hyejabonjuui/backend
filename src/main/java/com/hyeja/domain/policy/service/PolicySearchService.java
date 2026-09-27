package com.hyeja.domain.policy.service;

import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.dto.PolicySearchResponseDTO;
import com.hyeja.domain.policy.dto.PolicySearchResponseDTO.PolicyEligibilityStatusDTO;
import com.hyeja.domain.policy.dto.PolicySearchResponseDTO.PolicySearchItemDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.ReasonRequest;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.SearchIntent;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySearchService {
    private static final int SEARCH_LIMIT = 20;
    private static final Map<String, PolicyCategory> HASHTAG_CATEGORIES = Map.of(
            "#월세", PolicyCategory.MONTHLY_RENT,
            "#전세", PolicyCategory.JEONSE,
            "#청약", PolicyCategory.PURCHASE,
            "#공공임대", PolicyCategory.PUBLIC_RENT);

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProfileService profileService;
    private final PolicyEligibilityEvaluator eligibilityEvaluator;
    private final PolicySearchAiAnalyzer aiAnalyzer;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PolicySearchResponseDTO search(Long memberId, String rawQuery) {
        String query = rawQuery.trim();
        Profile profile = profileService.getActiveProfile(memberId);
        SearchIntent intent = resolveIntent(query);
        if (!intent.housingRelated()) {
            throw new GeneralException(ErrorStatus.POLICY_SEARCH_NOT_HOUSING);
        }
        Set<PolicyCategory> categories = intent.categories().isEmpty()
                ? Set.of(PolicyCategory.OTHER) : intent.categories();
        LocalDate today = LocalDate.now(clock);
        List<Policy> policies = policyRepository.searchActivePolicies(
                categories.contains(PolicyCategory.MONTHLY_RENT),
                categories.contains(PolicyCategory.JEONSE),
                categories.contains(PolicyCategory.PURCHASE),
                categories.contains(PolicyCategory.PUBLIC_RENT),
                categories.contains(PolicyCategory.OTHER),
                profile.getRegion().getRegionCode(),
                today,
                PageRequest.of(0, SEARCH_LIMIT, Sort.by(
                        Sort.Order.asc("applyEndDate").nullsLast(),
                        Sort.Order.asc("policyId"))));
        if (policies.isEmpty()) {
            throw new GeneralException(ErrorStatus.POLICY_SEARCH_EMPTY);
        }

        List<String> policyIds = policies.stream().map(Policy::getPolicyId).toList();
        Map<String, List<PolicyRegion>> regionsByPolicyId = policyRegionRepository
                .findAllActiveByPolicyIds(policyIds).stream()
                .collect(Collectors.groupingBy(
                        region -> region.getPolicy().getPolicyId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        Set<String> favoriteIds = favoriteRepository.findActivePolicyIds(memberId, policyIds);

        List<EvaluatedPolicy> evaluated = policies.stream()
                .map(policy -> evaluate(policy, profile,
                        regionsByPolicyId.getOrDefault(policy.getPolicyId(), List.of())))
                .toList();
        Map<String, String> reasons = generateReasons(evaluated, profile);

        List<PolicySearchItemDTO> approved = new ArrayList<>();
        List<PolicySearchItemDTO> underReview = new ArrayList<>();
        List<PolicySearchItemDTO> declined = new ArrayList<>();
        for (EvaluatedPolicy item : evaluated) {
            PolicySearchItemDTO dto = toDto(item, favoriteIds,
                    reasons.getOrDefault(item.policy().getPolicyId(), fallbackReason(item)));
            switch (item.overallStatus()) {
                case ABLE -> approved.add(dto);
                case UNKNOWN -> underReview.add(dto);
                case DISABLE -> declined.add(dto);
            }
        }
        return new PolicySearchResponseDTO(approved, underReview, declined);
    }

    private SearchIntent resolveIntent(String query) {
        PolicyCategory hashtagCategory = HASHTAG_CATEGORIES.get(query);
        return hashtagCategory == null
                ? aiAnalyzer.analyzeIntent(query)
                : new SearchIntent(true, Set.of(hashtagCategory));
    }

    private EvaluatedPolicy evaluate(Policy policy, Profile profile, List<PolicyRegion> regions) {
        List<PolicyRegion> matchingRegions = regions.stream()
                .filter(region -> matchesRegion(region.getRegion().getRegionCode(),
                        profile.getRegion().getRegionCode()))
                .toList();
        List<ConditionResultDTO> conditions = eligibilityEvaluator.evaluate(
                policy, profile, regions.isEmpty() ? List.of() : matchingRegions);
        EligibilityStatus overall = conditions.stream()
                .map(ConditionResultDTO::status)
                .filter(status -> status == EligibilityStatus.DISABLE)
                .findFirst()
                .orElseGet(() -> conditions.stream()
                        .map(ConditionResultDTO::status)
                        .filter(status -> status == EligibilityStatus.UNKNOWN)
                        .findFirst()
                        .orElse(EligibilityStatus.ABLE));
        return new EvaluatedPolicy(policy, conditions, overall);
    }

    private boolean matchesRegion(String policyRegionCode, String memberRegionCode) {
        return policyRegionCode.equals(memberRegionCode)
                || policyRegionCode.endsWith("000")
                && policyRegionCode.substring(0, 2).equals(memberRegionCode.substring(0, 2));
    }

    private Map<String, String> generateReasons(List<EvaluatedPolicy> evaluated, Profile profile) {
        try {
            return aiAnalyzer.generateReasons(evaluated.stream()
                    .map(item -> new ReasonRequest(
                            item.policy(), profile, item.overallStatus(), item.conditions()))
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("정책 검색 개인화 이유 생성에 실패해 기본 문장을 사용합니다.", exception);
            return Collections.emptyMap();
        }
    }

    private PolicySearchItemDTO toDto(EvaluatedPolicy evaluated, Set<String> favoriteIds,
            String reason) {
        Map<EligibilityConditionType, EligibilityStatus> statuses = new EnumMap<>(EligibilityConditionType.class);
        evaluated.conditions().forEach(condition -> statuses.put(condition.type(), condition.status()));
        return new PolicySearchItemDTO(
                evaluated.policy().getPolicyId(),
                evaluated.policy().getPolicyName(),
                evaluated.policy().getCategories(),
                evaluated.policy().getApplyEndDate(),
                evaluated.policy().getApplyPeriodCode(),
                favoriteIds.contains(evaluated.policy().getPolicyId()),
                reason,
                new PolicyEligibilityStatusDTO(
                        statuses.get(EligibilityConditionType.AGE),
                        statuses.get(EligibilityConditionType.REGION),
                        statuses.get(EligibilityConditionType.INCOME),
                        statuses.get(EligibilityConditionType.EMPLOYMENT),
                        statuses.get(EligibilityConditionType.HOUSELESS)));
    }

    private String fallbackReason(EvaluatedPolicy item) {
        ConditionResultDTO decisive = item.conditions().stream()
                .filter(condition -> condition.status() == item.overallStatus())
                .findFirst()
                .orElse(item.conditions().get(0));
        return switch (item.overallStatus()) {
            case ABLE -> "입력한 회원 정보로 확인 가능한 자격 조건을 모두 만족해요.";
            case UNKNOWN -> "%s 조건은 추가 확인이 필요해요.".formatted(conditionName(decisive.type()));
            case DISABLE -> "%s 조건은 %s이지만 현재 회원 정보는 %s예요.".formatted(
                    conditionName(decisive.type()), decisive.policyCondition(), decisive.memberValue());
        };
    }

    private String conditionName(EligibilityConditionType type) {
        return switch (type) {
            case AGE -> "나이";
            case REGION -> "지역";
            case INCOME -> "소득";
            case EMPLOYMENT -> "취업";
            case HOUSELESS -> "무주택";
        };
    }

    private record EvaluatedPolicy(
            Policy policy,
            List<ConditionResultDTO> conditions,
            EligibilityStatus overallStatus) {
    }
}
