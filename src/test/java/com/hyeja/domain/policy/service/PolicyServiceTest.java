package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyEmploymentCondition;
import com.hyeja.domain.policy.enums.PolicyMarriageCondition;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.profile.entity.Profile;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class PolicyServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final CardNewsRepository cardNewsRepository = mock(CardNewsRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final PolicyApiCodeConverter codeConverter = new PolicyApiCodeConverter();
    private final PolicyApiConverter apiConverter = new PolicyApiConverter(codeConverter);
    private final PolicyAiAnalyzer policyAiAnalyzer = mock(PolicyAiAnalyzer.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final PolicyRegionRepository policyRegionRepository = mock(PolicyRegionRepository.class);
    private final PolicyEligibilityEvaluator policyEligibilityEvaluator =
            new PolicyEligibilityEvaluator(new PolicyIncomeEligibilityEvaluator());
    private final PolicyService service = new PolicyService(
            policyRepository, cardNewsRepository, restTemplate, apiConverter, codeConverter,
            policyAiAnalyzer, memberRepository, profileRepository, policyRegionRepository,
            policyEligibilityEvaluator);

    @Test
    void mapsYouthPolicyApiFieldsToPolicyEntity() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("202609250001");
        item.setPolicyName(" 청년 주거 지원 ");
        item.setCategory("주거");
        item.setSubCategory("주택 및 거주지");
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

        Policy policy = apiConverter.convert(item, PolicyCategory.MONTHLY_RENT, true,
                PolicyIncomeCondition.COMPARABLE, 100, 300);

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
        assertThat(policy.getMarriageCode()).isEqualTo(PolicyMarriageCondition.MARRIED);
        assertThat(policy.getEmploymentCodes()).containsExactlyInAnyOrder(
                PolicyEmploymentCondition.EMPLOYED,
                PolicyEmploymentCondition.SELF_EMPLOYED);
        assertThat(policy.getHouselessYn()).isTrue();
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

        Policy policy = apiConverter.convert(item, PolicyCategory.OTHER, null,
                PolicyIncomeCondition.UNKNOWN, null, null);

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

        assertThat(apiConverter.convert(item, PolicyCategory.OTHER, null,
                PolicyIncomeCondition.UNKNOWN, null, null).getActiveYn()).isFalse();
    }

    @Test
    void mapsMarriageConditionCodesToReadableValues() {
        assertThat(mapMarriageCondition("0055001")).isEqualTo(PolicyMarriageCondition.MARRIED);
        assertThat(mapMarriageCondition("55002")).isEqualTo(PolicyMarriageCondition.SINGLE);
        assertThat(mapMarriageCondition("0055003"))
                .isEqualTo(PolicyMarriageCondition.NO_RESTRICTION);
        assertThat(mapMarriageCondition("unknown")).isNull();
        assertThat(mapMarriageCondition(" ")).isNull();
    }

    @Test
    void mapsEmploymentCodesToReadableValues() {
        assertThat(mapEmploymentConditions("0013001"))
                .containsExactly(PolicyEmploymentCondition.EMPLOYED);
        assertThat(mapEmploymentConditions("13002"))
                .containsExactly(PolicyEmploymentCondition.SELF_EMPLOYED);
        assertThat(mapEmploymentConditions("0013003"))
                .containsExactly(PolicyEmploymentCondition.UNEMPLOYED);
        assertThat(mapEmploymentConditions("13004"))
                .containsExactly(PolicyEmploymentCondition.FREELANCER);
        assertThat(mapEmploymentConditions("0013005"))
                .containsExactly(PolicyEmploymentCondition.DAILY_WORKER);
        assertThat(mapEmploymentConditions("13006"))
                .containsExactly(PolicyEmploymentCondition.ENTREPRENEUR);
        assertThat(mapEmploymentConditions("0013007"))
                .containsExactly(PolicyEmploymentCondition.SHORT_TERM_WORKER);
        assertThat(mapEmploymentConditions("13008"))
                .containsExactly(PolicyEmploymentCondition.FARMER);
        assertThat(mapEmploymentConditions("0013009"))
                .containsExactly(PolicyEmploymentCondition.OTHER);
        assertThat(mapEmploymentConditions("13010"))
                .containsExactly(PolicyEmploymentCondition.NO_RESTRICTION);
        assertThat(mapEmploymentConditions("0013001,0013004"))
                .containsExactlyInAnyOrder(
                        PolicyEmploymentCondition.EMPLOYED,
                        PolicyEmploymentCondition.FREELANCER);
        assertThat(mapEmploymentConditions("unknown")).isEmpty();
    }

    @Test
    void doesNotSaveNonApprovedHousingPolicy() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.com/policies");

        PolicyItem rejected = new PolicyItem();
        rejected.setPolicyId("rejected-policy");
        rejected.setPolicyName("반려된 주거 정책");
        rejected.setCategory("주거");
        rejected.setApprovalStatusCode("44003");

        PolicyApiResponseDTO.Pagging pagging = new PolicyApiResponseDTO.Pagging();
        pagging.setTotCount(1);
        PolicyApiResponseDTO.ResultData result = new PolicyApiResponseDTO.ResultData();
        result.setPagging(pagging);
        result.setYouthPolicyList(List.of(rejected));
        PolicyApiResponseDTO response = new PolicyApiResponseDTO();
        response.setResult(result);

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class))).thenReturn(response);

        assertThat(service.fetchAndSaveHousingPolicies()).isZero();
        verify(policyRepository, never()).save(org.mockito.ArgumentMatchers.any(Policy.class));
        verify(cardNewsRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(policyAiAnalyzer, never()).analyze(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void savesCategoryReturnedByAiClassifier() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.com/policies");

        PolicyItem item = new PolicyItem();
        item.setPolicyId("ai-policy");
        item.setPolicyName("청년 임대주택 정책");
        item.setCategory("주거");
        item.setApprovalStatusCode("44002");

        PolicyApiResponseDTO.Pagging pagging = new PolicyApiResponseDTO.Pagging();
        pagging.setTotCount(1);
        PolicyApiResponseDTO.ResultData result = new PolicyApiResponseDTO.ResultData();
        result.setPagging(pagging);
        result.setYouthPolicyList(List.of(item));
        PolicyApiResponseDTO response = new PolicyApiResponseDTO();
        response.setResult(result);

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class))).thenReturn(response);
        when(policyAiAnalyzer.analyze(item)).thenReturn(new PolicyAiAnalysis(
                PolicyCategory.PUBLIC_RENT, 0.95, "공공임대주택 입주 정책",
                true, 0.91, "무주택 세대구성원 조건이 명시됨",
                PolicyIncomeCondition.COMPARABLE, null, 50_000_000,
                0.90, "개인 연소득 5천만원 이하"));
        when(policyRepository.save(org.mockito.ArgumentMatchers.any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardNewsRepository.existsByPolicy_PolicyIdAndCardNo("ai-policy", 1L))
                .thenReturn(true);

        assertThat(service.fetchAndSaveHousingPolicies()).isEqualTo(1);
        verify(policyRepository).save(org.mockito.ArgumentMatchers.argThat(
                policy -> policy.getCategory() == PolicyCategory.PUBLIC_RENT
                        && Boolean.TRUE.equals(policy.getHouselessYn())
                        && policy.getIncomeConditionCode() == PolicyIncomeCondition.COMPARABLE
                        && policy.getIncomeMax() == 50_000_000));
    }

    @Test
    void storesCategoryProvidedByAiClassifier() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("ai-category-policy");
        item.setPolicyName("청년 주거 정책");

        Policy policy = apiConverter.convert(item, PolicyCategory.PUBLIC_RENT, null,
                PolicyIncomeCondition.UNKNOWN, null, null);

        assertThat(policy.getCategory()).isEqualTo(PolicyCategory.PUBLIC_RENT);
    }

    @Test
    void loadsAtMostConfiguredPages() {
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
        verify(restTemplate, times(1)).getForObject(
                org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class));
    }

    private PolicyMarriageCondition mapMarriageCondition(String code) {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("marriage-test");
        item.setPolicyName("혼인 조건 테스트");
        item.setCategory("주거");
        item.setMarriageCode(code);
        return apiConverter.convert(item, PolicyCategory.OTHER, null,
                PolicyIncomeCondition.UNKNOWN, null, null).getMarriageCode();
    }

    @Test
    void returnsPolicyDetailWithFiveMemberEligibilityConditions() {
        Policy policy = mock(Policy.class);
        when(policy.getPolicyId()).thenReturn("policy-detail");
        when(policy.getPolicyName()).thenReturn("청년 주거 정책");
        when(policy.getCategory()).thenReturn(PolicyCategory.OTHER);
        when(policy.getAgeLimitYn()).thenReturn(true);
        when(policy.getIncomeConditionCode()).thenReturn(PolicyIncomeCondition.UNKNOWN);

        Member member = mock(Member.class);
        when(member.getEmail()).thenReturn("member@example.com");
        Profile profile = mock(Profile.class);

        when(policyRepository.findById("policy-detail")).thenReturn(Optional.of(policy));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(profileRepository.findById("member@example.com")).thenReturn(Optional.of(profile));
        when(policyRegionRepository.findAllByPolicy_PolicyId("policy-detail"))
                .thenReturn(List.of());

        PolicyDetailResponseDTO response =
                service.getPolicyDetailForMember("policy-detail", 1L);

        assertThat(response.policyId()).isEqualTo("policy-detail");
        assertThat(response.conditions()).hasSize(5);
        assertThat(response.overallStatus()).isEqualTo(
                com.hyeja.domain.policy.enums.EligibilityStatus.U);
    }

    private Set<PolicyEmploymentCondition> mapEmploymentConditions(String codes) {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("employment-test");
        item.setPolicyName("취업 조건 테스트");
        item.setCategory("주거");
        item.setEmploymentCodes(codes);
        return apiConverter.convert(item, PolicyCategory.OTHER, null,
                PolicyIncomeCondition.UNKNOWN, null, null).getEmploymentCodes();
    }
}
