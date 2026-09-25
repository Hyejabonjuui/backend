package com.hyeja.domain.policy.converter;

import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyApiConverter {
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final PolicyApiCodeConverter codeConverter;

    public Policy convert(PolicyItem item) {
        DateRange dates = parseDateRange(item.getApplyYmd());
        return Policy.builder()
                .policyId(trimToNull(item.getPolicyId()))
                .policyName(defaultIfBlank(item.getPolicyName(), "제목 없음"))
                .category(classifyCategory(item))
                .apiSubCategory(trimToNull(item.getSubCategory()))
                .subtypeCode(null)
                .keywords(trimToNull(item.getKeywords()))
                .description(null)
                .supportContent(trimToNull(item.getSupportContent()))
                .minAge(parseNullableInteger(item.getMinAge()))
                .maxAge(parseNullableInteger(item.getMaxAge()))
                .ageLimitYn(toBoolean(item.getAgeLimitYn(), false))
                .incomeConditionCode(trimToNull(item.getIncomeConditionCode()))
                .incomeMin(parseNullableInteger(item.getIncomeMin()))
                .incomeMax(parseNullableInteger(item.getIncomeMax()))
                .incomeEtc(trimToNull(item.getIncomeEtc()))
                .marriageCode(codeConverter.convertMarriage(item.getMarriageCode()))
                .employmentCodes(codeConverter.convertEmployment(item.getEmploymentCodes()))
                .houselessYn(null)
                .housingType(trimToNull(item.getSubCategory()))
                .applyPeriodCode(defaultIfBlank(item.getApplyPeriodCode(), "UNKNOWN"))
                .extraQualification(joinNonBlank(
                        item.getExtraQualification(), item.getParticipantTarget()))
                .applyStartDate(dates.start())
                .applyEndDate(dates.end())
                .applyMethod(trimToNull(item.getApplyMethod()))
                .applyUrl(trimToNull(item.getApplyUrl()))
                .refUrl(firstNonBlank(item.getReferenceUrl1(), item.getReferenceUrl2()))
                .viewCount(parseInteger(item.getViewCount(), 0))
                .activeYn(codeConverter.isApproved(item.getApprovalStatusCode()))
                .build();
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

    private DateRange parseDateRange(String value) {
        String text = trimToNull(value);
        if (text == null) return new DateRange(null, null);
        LocalDate start = null;
        LocalDate end = null;
        for (String token : text.split("[^0-9]+")) {
            if (token.length() != 8) continue;
            try {
                LocalDate parsed = LocalDate.parse(token, BASIC_DATE);
                if (start == null) start = parsed;
                end = parsed;
            } catch (DateTimeParseException ignored) {
                // API의 비정형 날짜는 null로 두어 동기화 전체가 중단되지 않게 한다.
            }
        }
        return new DateRange(start, end);
    }

    private Integer parseNullableInteger(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }

    private String joinNonBlank(String first, String second) {
        String left = trimToNull(first);
        String right = trimToNull(second);
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

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
