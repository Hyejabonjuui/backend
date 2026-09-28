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
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.AiAssessment;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.ProfileSummary;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.ReasonRequest;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.SearchIntent;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.AnalysisException;
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
    private static final String APPROVED_REASON = "정책의 모든 조건을 충족하고 있으며, 신청가능해요.";
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
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new GeneralException(ErrorStatus.POLICY_SEARCH_QUERY_REQUIRED);
        }
        if (rawQuery.length() > 200) {
            throw new GeneralException(ErrorStatus.POLICY_SEARCH_QUERY_TOO_LONG);
        }
        String query = rawQuery.trim();
        Profile profile = profileService.getActiveProfile(memberId);
        SearchIntent intent = resolveIntent(query);
        if (!intent.housingRelated()) {
            throw new GeneralException(ErrorStatus.POLICY_SEARCH_NOT_HOUSING);
        }
        Set<PolicyCategory> categories = intent.categories().isEmpty()
                ? Set.of(PolicyCategory.values()) : intent.categories();
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
            return new PolicySearchResponseDTO(List.of(), List.of(), List.of());
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
        Map<String, AiAssessment> assessments = generateAssessments(evaluated, profile);

        List<PolicySearchItemDTO> approved = new ArrayList<>();
        List<PolicySearchItemDTO> underReview = new ArrayList<>();
        List<PolicySearchItemDTO> declined = new ArrayList<>();
        for (EvaluatedPolicy item : evaluated) {
            AiAssessment assessment = assessments.get(item.policy().getPolicyId());
            EvaluatedPolicy finalized = finalizeEvaluation(item, assessment);
            String reason = finalized.overallStatus() == EligibilityStatus.ABLE
                    ? APPROVED_REASON
                    : assessment != null && assessment.status() == finalized.overallStatus()
                    && isValidReason(finalized, assessment.reason(), evaluated)
                    ? assessment.reason() : fallbackReason(finalized);
            PolicySearchItemDTO dto = toDto(finalized, favoriteIds, reason);
            switch (finalized.overallStatus()) {
                case ABLE -> approved.add(dto);
                case UNKNOWN -> underReview.add(dto);
                case DISABLE -> declined.add(dto);
            }
        }
        return new PolicySearchResponseDTO(approved, underReview, declined);
    }

    private SearchIntent resolveIntent(String query) {
        PolicyCategory hashtagCategory = HASHTAG_CATEGORIES.get(query);
        if (hashtagCategory != null) {
            return new SearchIntent(true, Set.of(hashtagCategory));
        }
        try {
            return aiAnalyzer.analyzeIntent(query);
        } catch (AnalysisException exception) {
            log.warn("정책 검색 의도 분석에 실패해 전체 주거 정책으로 검색합니다.", exception);
            return new SearchIntent(true, Set.of());
        }
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

    private Map<String, AiAssessment> generateAssessments(
            List<EvaluatedPolicy> evaluated, Profile profile) {
        ProfileSummary profileSummary = new ProfileSummary(
                profile.getBirth() == null ? null : profile.getBirth().toString(),
                profile.getRegion() == null ? null : profile.getRegion().getSigunguName(),
                profile.getEmploymentCode() == null ? null : profile.getEmploymentCode().getLabel(),
                profile.getHouselessYn() == null ? null
                        : profile.getHouselessYn() ? "무주택" : "주택 소유",
                profile.getMarriageCode() == null ? null : profile.getMarriageCode().getLabel(),
                profile.getIncomeRangeCode() == null ? null : profile.getIncomeRangeCode().getLabel(),
                profile.getEducationCode() == null ? null : profile.getEducationCode().getLabel(),
                profile.getHousingType() == null ? null : profile.getHousingType().getLabel());
        try {
            return aiAnalyzer.assess(evaluated.stream()
                    .map(item -> new ReasonRequest(
                            item.policy().getPolicyId(), item.policy().getPolicyName(),
                            item.policy().getExtraQualification(), profileSummary,
                            item.overallStatus(), item.conditions()))
                    .toList());
        } catch (RuntimeException exception) {
            log.warn("정책 검색 개인화 이유 생성에 실패해 기본 문장을 사용합니다.", exception);
            return Collections.emptyMap();
        }
    }

    private EvaluatedPolicy finalizeEvaluation(
            EvaluatedPolicy item, AiAssessment assessment) {
        if (assessment == null || assessment.status() == null
                || !canDowngrade(item.overallStatus(), assessment.status())) {
            return item;
        }
        return new EvaluatedPolicy(item.policy(), item.conditions(), assessment.status());
    }

    private boolean canDowngrade(EligibilityStatus server, EligibilityStatus ai) {
        return switch (server) {
            case ABLE -> true;
            case UNKNOWN -> ai != EligibilityStatus.ABLE;
            case DISABLE -> ai == EligibilityStatus.DISABLE;
        };
    }

    private boolean isValidReason(
            EvaluatedPolicy item, String reason, List<EvaluatedPolicy> evaluated) {
        if (reason == null || !reason.endsWith("요.") || reason.contains("\n")) {
            return false;
        }
        if (evaluated.stream().anyMatch(candidate ->
                reason.contains(candidate.policy().getPolicyName()))) {
            return false;
        }
        return switch (item.overallStatus()) {
            case ABLE -> !containsAny(reason, "추가 확인", "신청할 수 없", "충족하지");
            case UNKNOWN -> !containsAny(reason, "신청 가능", "신청할 수 있어", "모든 조건을 충족", "신청할 수 없");
            case DISABLE -> !containsAny(reason, "신청 가능", "신청할 수 있어", "모든 조건을 충족", "추가 확인");
        };
    }

    private boolean containsAny(String value, String... candidates) {
        return java.util.Arrays.stream(candidates).anyMatch(value::contains);
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
        List<ConditionResultDTO> decisive = item.conditions().stream()
                .filter(condition -> condition.status() == item.overallStatus())
                .toList();
        return switch (item.overallStatus()) {
            case ABLE -> APPROVED_REASON;
            case UNKNOWN -> decisive.isEmpty()
                    ? "추가 자격 조건을 확인해야 해요."
                    : "%s 조건을 추가로 확인해야 해요.".formatted(conditionNames(decisive));
            case DISABLE -> decisive.isEmpty()
                    ? "추가 자격 조건이 회원 정보와 일치하지 않아요."
                    : decisive.stream()
                    .map(condition -> "%s 조건은 %s이지만 회원 정보는 %s예요"
                            .formatted(conditionName(condition.type()),
                                    condition.policyCondition(), condition.memberValue()))
                    .collect(Collectors.joining(", ", "", "."));
        };
    }

    private String conditionNames(List<ConditionResultDTO> conditions) {
        return conditions.stream()
                .map(condition -> conditionName(condition.type()))
                .collect(Collectors.joining(", "));
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
