package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicySyncItemService {
    private static final int CARD_BODY_MAX_LENGTH = 500;
    private static final Pattern REGION_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{5}(?!\\d)");

    private final PolicyRepository policyRepository;
    private final CardNewsRepository cardNewsRepository;
    private final PolicyApiConverter policyApiConverter;
    private final PolicyRegionRepository policyRegionRepository;
    private final RegionRepository regionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PolicyItem item, PolicyAiAnalysis analysis) {
        List<Region> regions = resolvePolicyRegions(item.getPolicyId(), item.getRegionCodes());
        Policy policy = policyRepository.save(policyApiConverter.convert(
                item, analysis.categories(), analysis.description(), analysis.houselessRequirement(),
                analysis.incomeCondition(), analysis.incomeMin(), analysis.incomeMax()));
        replacePolicyRegions(policy, regions);
        createMissingCardNews(policy, analysis);
    }

    private List<Region> resolvePolicyRegions(String policyId, String rawRegionCodes) {
        Set<String> regionCodes = parseRegionCodes(rawRegionCodes);
        regionCodes.remove("00000");
        return regionCodes.isEmpty() ? List.of() : resolveRegions(policyId, regionCodes);
    }

    private void replacePolicyRegions(Policy policy, List<Region> regions) {
        policyRegionRepository.deleteAllByPolicy_PolicyId(policy.getPolicyId());
        policyRegionRepository.saveAll(regions.stream()
                .map(region -> PolicyRegion.builder().policy(policy).region(region).build())
                .toList());
    }

    private List<Region> resolveRegions(String policyId, Set<String> regionCodes) {
        Map<String, Region> resolvedRegions = regionRepository.findAllById(regionCodes).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Region::getRegionCode, region -> region,
                        (left, right) -> left, java.util.LinkedHashMap::new));
        Set<String> unresolvedCodes = new LinkedHashSet<>(regionCodes);
        unresolvedCodes.removeAll(resolvedRegions.keySet());

        Set<String> unresolvedBroadCodes = unresolvedCodes.stream()
                .filter(this::isSidoCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String sidoCode : unresolvedBroadCodes) {
            Region representative = regionRepository
                    .findAllByRegionCodeStartingWith(sidoCode.substring(0, 2)).stream()
                    .filter(region -> !isSidoCode(region.getRegionCode()))
                    .findFirst()
                    .orElse(null);
            if (representative == null) continue;

            Region broadRegion = Region.builder()
                    .regionCode(sidoCode)
                    .sigunguName(sidoName(representative.getSigunguName()))
                    .build();
            regionRepository.save(broadRegion);
            resolvedRegions.put(sidoCode, broadRegion);
            unresolvedCodes.remove(sidoCode);
        }

        if (!unresolvedCodes.isEmpty()) {
            log.warn("정책 지역 코드를 region 테이블에서 찾지 못했습니다. policyId={}, codes={}",
                    policyId, unresolvedCodes);
            throw new IllegalArgumentException("정책 지역 코드를 찾을 수 없습니다: " + unresolvedCodes);
        }
        return List.copyOf(resolvedRegions.values());
    }

    private boolean isSidoCode(String regionCode) {
        return !"00000".equals(regionCode) && regionCode.endsWith("000");
    }

    private String sidoName(String sigunguName) {
        int separatorIndex = sigunguName.indexOf(' ');
        return separatorIndex < 0 ? sigunguName : sigunguName.substring(0, separatorIndex);
    }

    private Set<String> parseRegionCodes(String rawRegionCodes) {
        Set<String> regionCodes = new LinkedHashSet<>();
        if (rawRegionCodes == null || rawRegionCodes.isBlank()) {
            return regionCodes;
        }
        Matcher matcher = REGION_CODE_PATTERN.matcher(rawRegionCodes);
        if (matcher.find()) {
            regionCodes.add(matcher.group());
            if (matcher.find()) {
                log.warn("정책 지역 코드가 여러 개입니다. 첫 번째 코드만 저장합니다. rawCodes={}",
                        rawRegionCodes);
            }
        }
        return regionCodes;
    }

    private void createMissingCardNews(Policy policy, PolicyAiAnalysis analysis) {
        saveCardIfAbsent(policy, 1L, policy.getPolicyName(),
                defaultIfBlank(analysis.description(), policy.getSupportContent()));
        saveCardIfAbsent(policy, 2L, null,
                defaultIfBlank(analysis.eligibilityDescription(), policy.getExtraQualification()));
        saveCardIfAbsent(policy, 3L,
                defaultIfBlank(analysis.benefitTitle(), "지원 혜택"),
                defaultIfBlank(analysis.benefitDescription(), policy.getSupportContent()));
        saveCardIfAbsent(policy, 4L, formatApplyPeriod(policy),
                defaultIfBlank(analysis.applicationDescription(), policy.getApplyMethod()));
    }

    private void saveCardIfAbsent(Policy policy, Long cardNo, String title, String body) {
        if (cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), cardNo)) {
            return;
        }
        cardNewsRepository.save(CardNews.builder()
                .policy(policy)
                .title(limit(title, 255))
                .body(limit(defaultIfBlank(body, "등록된 내용이 없습니다."), CARD_BODY_MAX_LENGTH))
                .cardNo(cardNo)
                .build());
    }

    private String formatApplyPeriod(Policy policy) {
        if (policy.getApplyStartDate() != null && policy.getApplyEndDate() != null) {
            return "%s ~ %s".formatted(policy.getApplyStartDate(), policy.getApplyEndDate());
        }
        return policy.getApplyPeriodCode() == null
                ? "신청 기간 미정" : policy.getApplyPeriodCode().getLabel();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
