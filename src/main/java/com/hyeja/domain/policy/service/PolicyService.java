package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRepository;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20;
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PolicyRepository policyRepository;
    private final CardNewsRepository cardNewsRepository;
    private final RestTemplate restTemplate;

    @Value("${youth.api.key}") private String apiKey;
    @Value("${youth.api.url}") private String apiUrl;

    /** 온통청년 API의 전체 페이지를 조회해 주거 정책을 DB에 upsert한다. */
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
                if (!HOUSING_CATEGORY.equals(trimToNull(item.getCategory()))
                        || trimToNull(item.getPolicyId()) == null) continue;
                Policy policy = policyRepository.save(toPolicy(item));
                createTestCardNewsIfAbsent(policy);
                processedCount++;
            }
            pageNumber++;
        } while (pageNumber <= MAX_PAGES
                && (long) (pageNumber - 1) * PAGE_SIZE < totalCount);

        log.info("온통청년 1~{}페이지 중 주거 정책 {}건 적재 완료", MAX_PAGES, processedCount);
        return processedCount;
    }

    private PolicyApiResponseDTO requestPage(int pageNumber) {
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("apiKeyNm", apiKey)
                .queryParam("pageNum", pageNumber)
                .queryParam("pageSize", PAGE_SIZE)
                .queryParam("rtnType", "json").encode().build().toUri();
        log.info("온통청년 정책 API {}페이지 요청", pageNumber);
        return restTemplate.getForObject(uri, PolicyApiResponseDTO.class);
    }

    Policy toPolicy(PolicyItem item) {
        DateRange dates = parseDateRange(item.getApplyYmd());
        return Policy.builder()
                .policyId(trimToNull(item.getPolicyId()))
                .policyName(defaultIfBlank(item.getPolicyName(), "제목 없음"))
                .category(classifyCategory(item))
                .apiSubCategory(trimToNull(item.getSubCategory()))
                .subtypeCode(null)
                .keywords(trimToNull(item.getKeywords()))
                .description(null) // AI 요약용 컬럼
                .supportContent(trimToNull(item.getSupportContent()))
                .minAge(parseNullableInteger(item.getMinAge()))
                .maxAge(parseNullableInteger(item.getMaxAge()))
                .ageLimitYn(toBoolean(item.getAgeLimitYn(), false))
                .incomeConditionCode(trimToNull(item.getIncomeConditionCode()))
                .incomeMin(parseNullableInteger(item.getIncomeMin()))
                .incomeMax(parseNullableInteger(item.getIncomeMax()))
                .incomeEtc(trimToNull(item.getIncomeEtc()))
                .marriageCode(trimToNull(item.getMarriageCode()))
                .employmentCodes(trimToNull(item.getEmploymentCodes()))
                .houselessYn(null) // API에 직접 대응하는 필드 없음
                .housingType(trimToNull(item.getSubCategory()))
                .applyPeriodCode(defaultIfBlank(item.getApplyPeriodCode(), "UNKNOWN"))
                .extraQualification(joinNonBlank(item.getExtraQualification(), item.getParticipantTarget()))
                .applyStartDate(dates.start()).applyEndDate(dates.end())
                .applyMethod(trimToNull(item.getApplyMethod()))
                .applyUrl(trimToNull(item.getApplyUrl()))
                .refUrl(firstNonBlank(item.getReferenceUrl1(), item.getReferenceUrl2()))
                .viewCount(parseInteger(item.getViewCount(), 0))
                .activeYn(isActive(item.getApprovalStatusCode()))
                .build();
    }

    private void createTestCardNewsIfAbsent(Policy policy) {
        if (cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), 1L)) return;
        String body = defaultIfBlank(policy.getSupportContent(), "지원 내용이 등록되지 않았습니다.");
        if (body.length() > 500) body = body.substring(0, 497) + "...";
        cardNewsRepository.save(CardNews.builder().policy(policy).title(policy.getPolicyName())
                .body(body).cardNo(1L).build());
    }

    private DateRange parseDateRange(String value) {
        String text = trimToNull(value);
        if (text == null) return new DateRange(null, null);
        LocalDate start = null, end = null;
        for (String token : text.split("[^0-9]+")) {
            if (token.length() != 8) continue;
            try {
                LocalDate parsed = LocalDate.parse(token, BASIC_DATE);
                if (start == null) start = parsed;
                end = parsed;
            } catch (DateTimeParseException ignored) {
                log.debug("신청 기간 날짜 파싱 실패: {}", token);
            }
        }
        return new DateRange(start, end);
    }

    private Integer parseNullableInteger(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        try { return Integer.valueOf(text); } catch (NumberFormatException ignored) { return null; }
    }
    private int parseInteger(String value, int defaultValue) {
        Integer parsed = parseNullableInteger(value);
        return parsed == null ? defaultValue : parsed;
    }
    private boolean toBoolean(String value, boolean defaultValue) {
        String text = trimToNull(value);
        return text == null ? defaultValue
                : "Y".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text) || "1".equals(text);
    }
    private boolean isActive(String approvalStatusCode) {
        String code = trimToNull(approvalStatusCode);
        // 0044002는 API 코드표의 승인 상태다. 값이 없는 기존 응답은 활성으로 취급한다.
        return code == null || "0044002".equals(code);
    }
    private PolicyCategory classifyCategory(PolicyItem item) {
        String source = String.join(" ",
                defaultIfBlank(item.getPolicyName(), ""),
                defaultIfBlank(item.getKeywords(), ""),
                defaultIfBlank(item.getPolicyExplanation(), ""),
                defaultIfBlank(item.getSupportContent(), "")).toLowerCase();

        if (containsAny(source, "공공임대", "공공주택", "임대주택", "행복주택",
                "매입임대", "전세임대", "lh주택", "sh주택")) {
            return PolicyCategory.PUBLIC_RENT;
        }
        if (containsAny(source, "전세", "전세자금", "전세보증금", "보증금 지원", "보증금 대출")) {
            return PolicyCategory.JEONSE;
        }
        if (containsAny(source, "월세", "월 임대료", "주거급여", "임차료")) {
            return PolicyCategory.MONTHLY_RENT;
        }
        if (containsAny(source, "청약", "주택구입", "주택 구입", "내집마련", "내 집 마련")) {
            return PolicyCategory.PURCHASE;
        }
        return PolicyCategory.OTHER;
    }
    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }
    private String joinNonBlank(String first, String second) {
        String left = trimToNull(first), right = trimToNull(second);
        if (left == null) return right;
        if (right == null || left.equals(right)) return left;
        return left + System.lineSeparator() + right;
    }
    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
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

    private record DateRange(LocalDate start, LocalDate end) {}
}
