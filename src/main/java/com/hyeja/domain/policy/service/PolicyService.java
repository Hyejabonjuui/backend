package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.repository.PolicyRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {
    private static final String HOUSING_CATEGORY = "주거";
    private static final int PAGE_SIZE = 20; // 100
    private static final int MAX_PAGES = 1; // 20

    private final PolicyRepository policyRepository;
    private final CardNewsRepository cardNewsRepository;
    private final RestTemplate restTemplate;
    private final PolicyApiConverter policyApiConverter;
    private final PolicyApiCodeConverter policyApiCodeConverter;
    private final PolicyAiAnalyzer policyAiAnalyzer;

    @Value("${youth.api.key}")
    private String apiKey;

    @Value("${youth.api.url}")
    private String apiUrl;

    @Transactional
    public int fetchAndSaveHousingPolicies() {
        int pageNumber = 1;
        int totalCount = Integer.MAX_VALUE;
        int processedCount = 0;

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
                PolicyAiAnalysis analysis = policyAiAnalyzer.analyze(item);
                Policy policy = policyRepository.save(
                        policyApiConverter.convert(
                                item, analysis.category(), analysis.houselessYn(),
                                analysis.incomeCondition(), analysis.incomeMin(),
                                analysis.incomeMax()));
                createTestCardNewsIfAbsent(policy);
                processedCount++;
            }
            pageNumber++;
        } while (pageNumber <= MAX_PAGES
                && (long) (pageNumber - 1) * PAGE_SIZE < totalCount);

        log.info("온통청년 1~{}페이지 중 주거 정책 {}건 적재 완료", MAX_PAGES, processedCount);
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

    private void createTestCardNewsIfAbsent(Policy policy) {
        if (cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), 1L)) return;
        String body = defaultIfBlank(policy.getSupportContent(), "지원 내용이 등록되지 않았습니다.");
        if (body.length() > 500) body = body.substring(0, 497) + "...";
        cardNewsRepository.save(CardNews.builder()
                .policy(policy)
                .title(policy.getPolicyName())
                .body(body)
                .cardNo(1L)
                .build());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String text = trimToNull(value);
        return text == null ? defaultValue : text;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public List<Policy> getHousingPolicies() {
        return policyRepository.findAllByOrderByApplyEndDateAsc();
    }
}
