package com.hyeja.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyeja.domain.policy.enums.PolicyCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public final class PolicyGuestResponseDTO {

    private PolicyGuestResponseDTO() {
    }

    @Getter
    @Builder
    @Schema(name = "PolicyGuestRegionItemDTO", description = "비로그인 정책 대상 지역")
    public static class PolicyRegionItemDTO {

        @JsonProperty("region_code")
        @Schema(description = "시군구 코드", example = "11440")
        private String regionCode;

        @JsonProperty("region_name")
        @Schema(description = "시군구 이름", example = "서울특별시 마포구")
        private String regionName;
    }

    @Getter
    @Builder
    @Schema(name = "PolicyGuestListItemDTO", description = "비로그인 주거 정책 목록 항목")
    public static class PolicyListItemDTO {

        @JsonProperty("policy_id")
        @Schema(description = "정책 ID", example = "R202609230001")
        private String policyId;

        @JsonProperty("policy_name")
        @Schema(description = "정책명", example = "청년 월세 지원")
        private String policyName;

        @JsonProperty("category_code")
        @Schema(description = "정책 분류 코드", example = "MONTHLY_RENT")
        private PolicyCategory categoryCode;

        @JsonProperty("category_name")
        @Schema(description = "정책 분류 이름", example = "월세")
        private String categoryName;

        @Schema(description = "정책 대상 지역. 빈 배열이면 전국 정책")
        private List<PolicyRegionItemDTO> regions;

        @Schema(description = "전국 정책 여부", example = "false")
        private boolean nationwide;

        @JsonProperty("apply_end_date")
        @Schema(description = "신청 마감일. 상시 모집이면 null", example = "2026-09-30", nullable = true)
        private LocalDate applyEndDate;

        @JsonProperty("apply_period_code")
        @Schema(description = "신청 기간 구분 코드", example = "0057001")
        private String applyPeriodCode;

        @JsonProperty("d_day")
        @Schema(description = "마감일까지 남은 일수. 상시 모집이면 null", example = "4", nullable = true)
        private Integer dDay;

        @JsonProperty("d_day")
        public Integer getDDay() {
            return dDay;
        }
    }

    @Getter
    @Builder
    @Schema(name = "PolicyGuestListDTO", description = "비로그인 주거 정책 페이지 응답")
    public static class PolicyListDTO {

        @Schema(description = "현재 페이지의 정책 목록")
        private List<PolicyListItemDTO> policies;

        @Schema(description = "현재 페이지 번호", example = "0")
        private int page;

        @Schema(description = "페이지당 정책 개수", example = "8")
        private int size;

        @Schema(description = "전체 정책 개수", example = "24")
        private long totalElements;

        @Schema(description = "전체 페이지 수", example = "3")
        private int totalPages;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;
    }
}
