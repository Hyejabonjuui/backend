package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyConverter;
import com.hyeja.domain.policy.converter.PolicyGuestConverter;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.TermSummaryDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.term.repository.TermRepository;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {
    private static final String HOUSING_CATEGORY = "주거";
    private static final int PAGE_SIZE = 100; // 100
    // 끝은 totCount로 판단합니다. 이 값은 totCount가 비정상일 때 무한 반복을 막는 안전장치입니다.
    // 2026-09-27 기준 전체 2,934건(30페이지)이라 여유를 두고 50으로 둡니다.
    private static final int MAX_PAGES = 50;

    private final PolicyRepository policyRepository;
    private final RestTemplate restTemplate;
    private final PolicyApiCodeConverter policyApiCodeConverter;
    private final PolicyAiAnalyzer policyAiAnalyzer;
    private final PolicySyncItemService policySyncItemService;
    private final ProfileService profileService;
    private final PolicyRegionRepository policyRegionRepository;
    private final PolicyEligibilityEvaluator policyEligibilityEvaluator;
    private final FavoriteRepository favoriteRepository;
    private final TermRepository termRepository;
    private final Clock clock;

    @Value("${youth.api.key}")
    private String apiKey;

    @Value("${youth.api.url}")
    private String apiUrl;

    public int fetchAndSaveHousingPolicies() {
        int pageNumber = 1;
        int totalCount = Integer.MAX_VALUE;
        int processedCount = 0;
        int failedCount = 0;

        do {
            PolicyApiResponseDTO response = requestPageWithRetry(pageNumber);
            if (response == null || response.getResult() == null) {
                // 끝까지 못 가고 멈춘 것을 "완료"로 응답하지 않도록 알립니다. 이미 저장한 정책은 항목마다 따로 저장돼 남습니다.
                log.error("온통청년 API {}페이지 요청 실패로 수집 중단 - 저장 {}건", pageNumber, processedCount);
                throw new GeneralException(ErrorStatus.POLICY_SYNC_STOPPED,
                        Map.of("stoppedPage", pageNumber, "savedCount", processedCount));
            }
            if (response.getResult().getPagging() != null) {
                totalCount = response.getResult().getPagging().getTotCount();
            }
            List<PolicyItem> items = response.getResult().getYouthPolicyList();
            if (items == null || items.isEmpty()) break;

            int housingCount = 0;
            for (PolicyItem item : items) {
                if (!isSavableHousingPolicy(item)) continue;
                housingCount++;
                try {
                    PolicyAiAnalysis analysis = policyAiAnalyzer.analyze(item);
                    policySyncItemService.save(item, analysis);
                    processedCount++;
                } catch (Exception exception) {
                    failedCount++;
                    log.error("정책 동기화 항목 처리 실패 - page={}, policyId={}, policyName={}",
                            pageNumber, item.getPolicyId(), item.getPolicyName(), exception);
                }
            }
            log.info("온통청년 {}페이지 - totCount={}, 주거 정책 {}건", pageNumber, totalCount, housingCount);
            pageNumber++;
        } while (pageNumber <= MAX_PAGES
                && (long) (pageNumber - 1) * PAGE_SIZE < totalCount);

        log.info("온통청년 정책 적재 완료 - maxPages={}, successCount={}, failureCount={}",
                MAX_PAGES, processedCount, failedCount);
        return processedCount;
    }

    private boolean isSavableHousingPolicy(PolicyItem item) {
        return HOUSING_CATEGORY.equals(trimToNull(item.getCategory()))
                && trimToNull(item.getPolicyId()) != null
                && policyApiCodeConverter.isApproved(item.getApprovalStatusCode());
    }

    // 온통청년 서버가 뒤쪽 페이지에서 연결을 끊는 경우가 있어 한 번 더 시도합니다.
    // 그래도 실패하면 null을 돌려, 호출한 쪽이 수집 중단(POLICY_002)으로 응답합니다.
    private PolicyApiResponseDTO requestPageWithRetry(int pageNumber) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return requestPage(pageNumber);
            } catch (RestClientException exception) {
                // 예외 메시지에는 API 키가 담긴 요청 URL이 들어 있어, 원인 메시지만 남깁니다.
                log.warn("온통청년 API {}페이지 요청 실패 ({}번째 시도) - {}: {}", pageNumber, attempt,
                        exception.getClass().getSimpleName(), exception.getMostSpecificCause().getMessage());
            }
        }
        return null;
    }

    private PolicyApiResponseDTO requestPage(int pageNumber) {
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("apiKeyNm", apiKey)
                .queryParam("pageNum", pageNumber)
                .queryParam("pageSize", PAGE_SIZE)
                .queryParam("rtnType", "json")
                .encode()
                .build()
                .toUri();
        log.info("온통청년 정책 API {}페이지 요청", pageNumber);
        return restTemplate.getForObject(uri, PolicyApiResponseDTO.class);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public PolicyGuestResponseDTO.PolicyListDTO getGuestHousingPolicies(
            PolicyCategory category,
            PolicySort sort,
            int page,
            int size
    ) {
        LocalDate today = LocalDate.now(clock);
        Page<Policy> policyPage = policyRepository.findGuestHousingPolicies(
                category == null ? null : category.name(),
                today,
                PageRequest.of(page, size, sort.toSort())
        );
        List<String> policyIds = policyPage.getContent().stream()
                .map(Policy::getPolicyId)
                .toList();
        Map<String, List<Region>> regionsByPolicyId = policyIds.isEmpty()
                ? Collections.emptyMap()
                : policyRegionRepository.findAllActiveByPolicyIds(policyIds).stream()
                        .collect(Collectors.groupingBy(
                                policyRegion -> policyRegion.getPolicy().getPolicyId(),
                                Collectors.mapping(PolicyRegion::getRegion, Collectors.toList())
                        ));

        return PolicyGuestConverter.toPolicyListDTO(policyPage, regionsByPolicyId, today);
    }

    @Transactional(readOnly = true)
    public PolicyListDTO getHousingPoliciesForMember(
            Long memberId,
            PolicyCategory category,
            PolicySort sort,
            boolean onlyEligible,
            int page,
            int size
    ) {
        Profile profile = profileService.getActiveProfile(memberId);
        LocalDate today = LocalDate.now(clock);
        Page<Policy> policyPage = policyRepository.findHousingPoliciesForMember(
                category == null ? null : category.name(),
                onlyEligible,
                today,
                Period.between(profile.getBirth(), today).getYears(),
                profile.getHouselessYn(),
                profile.getEmploymentCode().name(),
                profile.getRegion().getRegionCode(),
                PageRequest.of(page, size, sort.toSort())
        );

        List<String> policyIds = policyPage.getContent().stream()
                .map(Policy::getPolicyId)
                .toList();
        Map<String, List<Region>> regionsByPolicyId = policyIds.isEmpty()
                ? Collections.emptyMap()
                : policyRegionRepository.findAllActiveByPolicyIds(policyIds).stream()
                        .collect(Collectors.groupingBy(
                                policyRegion -> policyRegion.getPolicy().getPolicyId(),
                                Collectors.mapping(PolicyRegion::getRegion, Collectors.toList())
                        ));
        Set<String> favoritePolicyIds = policyIds.isEmpty()
                ? Collections.emptySet()
                : favoriteRepository.findActivePolicyIds(memberId, policyIds);

        return PolicyConverter.toPolicyListDTO(
                policyPage, regionsByPolicyId, favoritePolicyIds, today);
    }

    @Transactional(readOnly = true)
    // memberId가 null이면 비로그인 조회입니다. 회원 조건 판정 없이 정책 정보만 내려줍니다
    // (conditions 빈 목록, overallStatus null, isFavorite false).
    public PolicyDetailResponseDTO getPolicyDetailForMember(String policyId, Long memberId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POLICY_NOT_FOUND));
        String extraQualification = policy.getExtraQualification();
        List<TermSummaryDTO> terms = extraQualification == null
                ? List.of()
                : termRepository.findAllByDeletedAtIsNullOrderByTermIdAsc().stream()
                        .filter(term -> extraQualification.contains(term.getTerm()))
                        .map(term -> new TermSummaryDTO(term.getTermId(), term.getTerm()))
                        .toList();
        List<ConditionResultDTO> conditions = List.of();
        boolean favorite = false;
        if (memberId != null) {
            // 목록 조회와 같은 기준으로, 탈퇴 회원은 MEMBER_001, 조건이 없거나 삭제됐으면 PROFILE_001입니다.
            Profile profile = profileService.getActiveProfile(memberId);
            List<PolicyRegion> policyRegions =
                    policyRegionRepository.findAllByPolicy_PolicyId(policyId);
            conditions = policyEligibilityEvaluator.evaluate(policy, profile, policyRegions);
            favorite = favoriteRepository.existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                    memberId, policyId);
        }

        return new PolicyDetailResponseDTO(
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getCategories(),
                policy.getCategories().stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .map(com.hyeja.domain.policy.enums.PolicyCategory::getLabel)
                        .toList(),
                policy.getApiSubCategory(),
                policy.getKeywords(),
                policy.getDescription(),
                policy.getSupportContent(),
                extraQualification,
                terms,
                policy.getApplyPeriodCode(),
                policy.getApplyPeriodCode() == null
                        ? null : policy.getApplyPeriodCode().getLabel(),
                policy.getApplyStartDate(),
                policy.getApplyEndDate(),
                policy.getApplyMethod(),
                policy.getApplyUrl(),
                policy.getRefUrl(),
                policy.getActiveYn(),
                favorite,
                memberId == null ? null : overallStatus(conditions),
                conditions);
    }

    private EligibilityStatus overallStatus(List<ConditionResultDTO> conditions) {
        if (conditions.stream().anyMatch(
                condition -> condition.status() == EligibilityStatus.DISABLE)) {
            return EligibilityStatus.DISABLE;
        }
        if (conditions.stream().anyMatch(
                condition -> condition.status() == EligibilityStatus.UNKNOWN)) {
            return EligibilityStatus.UNKNOWN;
        }
        return EligibilityStatus.ABLE;
    }
}
