package com.hyeja.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class PolicyApiResponseDTO {

    private int resultCode;
    private String resultMessage;
    private ResultData result;

    @Getter
    @Setter
    @ToString
    public static class ResultData {
        private Pagging pagging;
        
        @JsonProperty("youthPolicyList")
        private List<PolicyItem> youthPolicyList;
    }

    @Getter
    @Setter
    @ToString
    public static class Pagging {
        private int totCount;
        private int pageNum;
        private int pageSize;
    }

    @Getter
    @Setter
    @ToString
    public static class PolicyItem {
        @JsonProperty("plcyNo")
        private String policyId;          // 정책 ID

        @JsonProperty("plcyNm")
        private String policyName;        // 정책명

        @JsonProperty("lclsfNm")
        private String category;          // 정책 대분야 (예: 주거, 일자리 등)

        @JsonProperty("plcyExplnCn")
        private String description;       // 정책 설명

        @JsonProperty("plcySprtCn")
        private String supportContent;    // 지원 내용

        @JsonProperty("aplyUrlAddr")
        private String applyUrl;          // 신청 URL

        @JsonProperty("aplyYmd")
        private String applyYmd;          // 신청 기간 (예: "20260923 ~ 20260928")
    }
}