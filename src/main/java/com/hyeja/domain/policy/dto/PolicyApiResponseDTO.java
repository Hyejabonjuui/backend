package com.hyeja.domain.policy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyApiResponseDTO {

    @JsonProperty("bizId")
    private String policyId;          // 정책 ID

    @JsonProperty("polyBizSjnm")
    private String policyName;        // 정책명

    @JsonProperty("plcyTpNm")
    private String category;          // 정책분야 (주거 등)

    @JsonProperty("plcyExplnCn")
    private String description;       // 정책소개 (description)

    @JsonProperty("supportCn")
    private String supportContent;    // 지원내용

    @JsonProperty("polyRlmCd")
    private String apiSubCategory;

    @JsonProperty("rqutPrdCn")
    private String applyPeriodCode;   // 신청기간 내용 등
}