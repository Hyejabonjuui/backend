package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class PolicyServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final CardNewsRepository cardNewsRepository = mock(CardNewsRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final PolicyService service = new PolicyService(
            policyRepository, cardNewsRepository, restTemplate);

    @Test
    void mapsYouthPolicyApiFieldsToPolicyEntity() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("202609250001");
        item.setPolicyName(" 청년 주거 지원 ");
        item.setCategory("주거");
        item.setSubCategory("주택 및 거주지");
        item.setSubBusinessCode("0014005");
        item.setKeywords("청년,월세");
        item.setPolicyExplanation("API에서 받은 원문 설명");
        item.setSupportContent("월 20만 원 지원");
        item.setMinAge("19"); item.setMaxAge("39"); item.setAgeLimitYn("Y");
        item.setIncomeConditionCode("0043002");
        item.setIncomeMin("100"); item.setIncomeMax("300");
        item.setIncomeEtc("기준중위소득 기준");
        item.setMarriageCode("0055001");
        item.setEmploymentCodes("0013001,0013002");
        item.setApplyPeriodCode("0057001");
        item.setExtraQualification("서울 거주");
        item.setParticipantTarget("무주택 청년");
        item.setApplyYmd("20260901 ~ 20260930");
        item.setApplyMethod("온라인 신청");
        item.setApplyUrl("https://example.com/apply");
        item.setReferenceUrl1("https://example.com/reference");
        item.setViewCount("123");
        item.setApprovalStatusCode("0044002");

        Policy policy = service.toPolicy(item);

        assertThat(policy.getPolicyId()).isEqualTo("202609250001");
        assertThat(policy.getPolicyName()).isEqualTo("청년 주거 지원");
        assertThat(policy.getApiSubCategory()).isEqualTo("주택 및 거주지");
        assertThat(policy.getCategory()).isEqualTo(PolicyCategory.MONTHLY_RENT);
        assertThat(policy.getSubtypeCode()).isNull();
        assertThat(policy.getDescription()).isNull();
        assertThat(policy.getSupportContent()).isEqualTo("월 20만 원 지원");
        assertThat(policy.getMinAge()).isEqualTo(19);
        assertThat(policy.getMaxAge()).isEqualTo(39);
        assertThat(policy.getAgeLimitYn()).isTrue();
        assertThat(policy.getIncomeMin()).isEqualTo(100);
        assertThat(policy.getIncomeMax()).isEqualTo(300);
        assertThat(policy.getEmploymentCodes()).isEqualTo("0013001,0013002");
        assertThat(policy.getHouselessYn()).isNull();
        assertThat(policy.getHousingType()).isEqualTo("주택 및 거주지");
        assertThat(policy.getExtraQualification()).contains("서울 거주", "무주택 청년");
        assertThat(policy.getApplyStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(policy.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(policy.getViewCount()).isEqualTo(123);
        assertThat(policy.getActiveYn()).isTrue();
    }

    @Test
    void safelyMapsBlankAndNonNumericOptionalValues() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("policy-2"); item.setPolicyName(" "); item.setCategory("주거");
        item.setMinAge("-"); item.setApplyYmd("2026년 연중");

        Policy policy = service.toPolicy(item);

        assertThat(policy.getPolicyName()).isEqualTo("제목 없음");
        assertThat(policy.getMinAge()).isNull();
        assertThat(policy.getApplyStartDate()).isNull();
        assertThat(policy.getApplyEndDate()).isNull();
        assertThat(policy.getApplyPeriodCode()).isEqualTo("UNKNOWN");
        assertThat(policy.getViewCount()).isZero();
    }

    @Test
    void mapsNonApprovedPolicyAsInactive() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("policy-3");
        item.setPolicyName("승인 대기 정책");
        item.setCategory("주거");
        item.setApprovalStatusCode("NOT_APPROVED");

        assertThat(service.toPolicy(item).getActiveYn()).isFalse();
    }

    @Test
    void classifiesHousingPolicyCategoriesByPolicyContent() {
        assertThat(mapCategory("청년 공공임대주택 입주자 모집")).isEqualTo(PolicyCategory.PUBLIC_RENT);
        assertThat(mapCategory("전세보증금 대출이자 지원")).isEqualTo(PolicyCategory.JEONSE);
        assertThat(mapCategory("청년월세 한시 특별지원")).isEqualTo(PolicyCategory.MONTHLY_RENT);
        assertThat(mapCategory("청년 주택청약 교육")).isEqualTo(PolicyCategory.PURCHASE);
        assertThat(mapCategory("대학생 기숙사비 지원")).isEqualTo(PolicyCategory.OTHER);
    }

    @Test
    void loadsAtMostTwentyPages() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.com/policies");

        PolicyItem nonHousing = new PolicyItem();
        nonHousing.setPolicyId("non-housing");
        nonHousing.setCategory("일자리");

        PolicyApiResponseDTO.Pagging pagging = new PolicyApiResponseDTO.Pagging();
        pagging.setTotCount(10_000);
        PolicyApiResponseDTO.ResultData result = new PolicyApiResponseDTO.ResultData();
        result.setPagging(pagging);
        result.setYouthPolicyList(List.of(nonHousing));
        PolicyApiResponseDTO response = new PolicyApiResponseDTO();
        response.setResult(result);

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class))).thenReturn(response);

        assertThat(service.fetchAndSaveHousingPolicies()).isZero();
        verify(restTemplate, times(20)).getForObject(
                org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class));
    }

    private PolicyCategory mapCategory(String policyName) {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("category-test");
        item.setPolicyName(policyName);
        item.setCategory("주거");
        item.setSubCategory("전월세 및 주거급여 지원");
        return service.toPolicy(item).getCategory();
    }
}
