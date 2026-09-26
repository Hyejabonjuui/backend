package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyConverter;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.net.URI;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {
    private static final String HOUSING_CATEGORY = "주거";
    private static final int PAGE_SIZE = 100; // 100
    private static final int MAX_PAGES = 20; // 20

    private final PolicyRepository policyRepository;
    private final RestTemplate restTemplate;
    private final PolicyApiCodeConverter policyApiCodeConverter;
    private final PolicyAiAnalyzer policyAiAnalyzer;
    private final PolicySyncItemService policySyncItemService;
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final PolicyEligibilityEvaluator policyEligibilityEvaluator;
    private final FavoriteRepository favoriteRepository;

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
            PolicyApiResponseDTO response = requestPage(pageNumber);
            if (response == null || response.getResult() == null) {
                log.warn("온통청년 API {}페이지의 응답이 비어 있습니다.", pageNumber);
                break;
            }
            if (response.getResult().getPagging() != null) {
                totalCount = response.getResult().getPagging().getTotCount();
            }
            List<PolicyItem> items = response.getResult().getYouthPolicyList();
            if (items == null || items.isEmpty()) break;

            for (PolicyItem item : items) {
                if (!isSavableHousingPolicy(item)) continue;
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
    public List<Policy> getHousingPolicies() {
        return policyRepository.findAllByOrderByApplyEndDateAsc();
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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Profile profile = profileRepository.findById(member.getEmail())
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROFILE_NOT_FOUND));
        LocalDate today = LocalDate.now();
        Page<Policy> policyPage = policyRepository.findHousingPoliciesForMember(
                category,
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
    public PolicyDetailResponseDTO getPolicyDetailForMember(String policyId, Long memberId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.POLICY_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        Profile profile = profileRepository.findById(member.getEmail())
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROFILE_NOT_FOUND));
        List<PolicyRegion> policyRegions =
                policyRegionRepository.findAllByPolicy_PolicyId(policyId);
        List<ConditionResultDTO> conditions =
                policyEligibilityEvaluator.evaluate(policy, profile, policyRegions);

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
                policy.getExtraQualification(),
                policy.getApplyPeriodCode(),
                policy.getApplyPeriodCode() == null
                        ? null : policy.getApplyPeriodCode().getLabel(),
                policy.getApplyStartDate(),
                policy.getApplyEndDate(),
                policy.getApplyMethod(),
                policy.getApplyUrl(),
                policy.getRefUrl(),
                policy.getActiveYn(),
                favoriteRepository.existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(
                        memberId, policyId),
                overallStatus(conditions),
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
