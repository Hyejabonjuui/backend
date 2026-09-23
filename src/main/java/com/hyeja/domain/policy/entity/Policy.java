package com.hyeja.domain.policy.entity;

import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy extends BaseEntity {

    @Id
    @Column(name = "policy_id", nullable = false, length = 30, updatable = false)
    private String policyId;

    @Column(name = "policy_name", nullable = false, length = 200)
    private String policyName;

    // 기존 VARCHAR 컬럼을 유지하면서 enum 이름을 저장합니다.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 20)
    private PolicyCategory category;

    @Column(name = "api_sub_category", length = 50)
    private String apiSubCategory;

    @Column(name = "subtype_code", length = 20)
    private String subtypeCode;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "support_content", columnDefinition = "TEXT")
    private String supportContent;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "age_limit_yn", nullable = false)
    private Boolean ageLimitYn;

    @Column(name = "income_condition_code", length = 10)
    private String incomeConditionCode;

    @Column(name = "income_min")
    private Integer incomeMin;

    @Column(name = "income_max")
    private Integer incomeMax;

    @Column(name = "income_etc", columnDefinition = "TEXT")
    private String incomeEtc;

    @Column(name = "marriage_code", length = 10)
    private String marriageCode;

    @Column(name = "employment_codes", length = 200)
    private String employmentCodes;

    @Column(name = "houseless_yn")
    private Boolean houselessYn;

    @Column(name = "housing_type", length = 20)
    private String housingType;

    @Column(name = "apply_period_code", nullable = false, length = 10)
    private String applyPeriodCode;

    @Column(name = "extra_qualification", columnDefinition = "TEXT")
    private String extraQualification;

    @Column(name = "apply_start_date")
    private LocalDate applyStartDate;

    @Column(name = "apply_end_date")
    private LocalDate applyEndDate;

    @Column(name = "apply_method", columnDefinition = "TEXT")
    private String applyMethod;

    @Column(name = "apply_url", length = 500)
    private String applyUrl;

    @Column(name = "ref_url", length = 500)
    private String refUrl;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "active_yn", nullable = false)
    private Boolean activeYn;

    @Builder
    public Policy(String policyId, String policyName, PolicyCategory category, String apiSubCategory,
            String subtypeCode, String keywords, String description, String supportContent,
            Integer minAge, Integer maxAge, Boolean ageLimitYn, String incomeConditionCode,
            Integer incomeMin, Integer incomeMax, String incomeEtc, String marriageCode,
            String employmentCodes, Boolean houselessYn, String housingType, String applyPeriodCode,
            String extraQualification, LocalDate applyStartDate, LocalDate applyEndDate,
            String applyMethod, String applyUrl, String refUrl) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.category = category;
        this.apiSubCategory = apiSubCategory;
        this.subtypeCode = subtypeCode;
        this.keywords = keywords;
        this.description = description;
        this.supportContent = supportContent;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.ageLimitYn = ageLimitYn;
        this.incomeConditionCode = incomeConditionCode;
        this.incomeMin = incomeMin;
        this.incomeMax = incomeMax;
        this.incomeEtc = incomeEtc;
        this.marriageCode = marriageCode;
        this.employmentCodes = employmentCodes;
        this.houselessYn = houselessYn;
        this.housingType = housingType;
        this.applyPeriodCode = applyPeriodCode;
        this.extraQualification = extraQualification;
        this.applyStartDate = applyStartDate;
        this.applyEndDate = applyEndDate;
        this.applyMethod = applyMethod;
        this.applyUrl = applyUrl;
        this.refUrl = refUrl;
        this.viewCount = 0;
        this.activeYn = true;
    }
}
