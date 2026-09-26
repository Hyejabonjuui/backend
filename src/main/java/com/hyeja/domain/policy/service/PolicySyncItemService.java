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
import java.util.LinkedHashMap;
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
        Policy policy = policyRepository.save(policyApiConverter.convert(
                item, analysis.category(), analysis.houselessYn(),
                analysis.incomeCondition(), analysis.incomeMin(), analysis.incomeMax()));
        replacePolicyRegions(policy, item.getRegionCodes());
        createTestCardNewsIfAbsent(policy);
    }

    private void replacePolicyRegions(Policy policy, String rawRegionCodes) {
        policyRegionRepository.deleteAllByPolicy_PolicyId(policy.getPolicyId());

        Set<String> regionCodes = parseRegionCodes(rawRegionCodes);
        if (regionCodes.isEmpty()) {
            return;
        }

        List<Region> regions = resolveRegions(policy.getPolicyId(), regionCodes);

        policyRegionRepository.saveAll(regions.stream()
                .map(region -> PolicyRegion.builder().policy(policy).region(region).build())
                .toList());
    }

    private List<Region> resolveRegions(String policyId, Set<String> regionCodes) {
        Map<String, Region> resolvedRegions = new LinkedHashMap<>();
        Set<String> exactCodes = regionCodes.stream()
                .filter(code -> !isSidoCode(code))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (!exactCodes.isEmpty()) {
            regionRepository.findAllById(exactCodes).forEach(region ->
                    resolvedRegions.put(region.getRegionCode(), region));
        }

        Set<String> unresolvedCodes = new LinkedHashSet<>(exactCodes);
        unresolvedCodes.removeAll(resolvedRegions.keySet());

        regionCodes.stream().filter(this::isSidoCode).forEach(sidoCode -> {
            List<Region> childRegions = regionRepository.findAllByRegionCodeStartingWith(
                    sidoCode.substring(0, 2));
            if (childRegions.isEmpty()) {
                unresolvedCodes.add(sidoCode);
                return;
            }
            childRegions.forEach(region ->
                    resolvedRegions.put(region.getRegionCode(), region));
        });

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

    private Set<String> parseRegionCodes(String rawRegionCodes) {
        Set<String> regionCodes = new LinkedHashSet<>();
        if (rawRegionCodes == null || rawRegionCodes.isBlank()) {
            return regionCodes;
        }
        Matcher matcher = REGION_CODE_PATTERN.matcher(rawRegionCodes);
        while (matcher.find()) {
            regionCodes.add(matcher.group());
        }
        return regionCodes;
    }

    private void createTestCardNewsIfAbsent(Policy policy) {
        if (cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), 1L)) {
            return;
        }

        String body = defaultIfBlank(policy.getSupportContent(), "지원 내용이 등록되지 않았습니다.");
        if (body.length() > CARD_BODY_MAX_LENGTH) {
            body = body.substring(0, CARD_BODY_MAX_LENGTH - 3) + "...";
        }

        cardNewsRepository.save(CardNews.builder()
                .policy(policy)
                .title(policy.getPolicyName())
                .body(body)
                .cardNo(1L)
                .build());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
