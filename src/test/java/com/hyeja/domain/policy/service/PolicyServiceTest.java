package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyEmploymentCondition;
import com.hyeja.domain.policy.enums.PolicyMarriageCondition;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.region.entity.Region;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class PolicyServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final PolicyApiCodeConverter codeConverter = new PolicyApiCodeConverter();
    private final PolicyApiConverter apiConverter = new PolicyApiConverter(codeConverter);
    private final PolicyAiAnalyzer policyAiAnalyzer = mock(PolicyAiAnalyzer.class);
    private final PolicySyncItemService policySyncItemService = mock(PolicySyncItemService.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final PolicyRegionRepository policyRegionRepository = mock(PolicyRegionRepository.class);
    private final PolicyEligibilityEvaluator policyEligibilityEvaluator =
            new PolicyEligibilityEvaluator(new PolicyIncomeEligibilityEvaluator());
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-27T00:00:00Z"),
            ZoneId.of("Asia/Seoul"));
    private final PolicyService service = new PolicyService(
            policyRepository, restTemplate, codeConverter, policyAiAnalyzer,
            policySyncItemService, memberRepository, profileRepository, policyRegionRepository,
            policyEligibilityEvaluator, favoriteRepository, clock);

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
    void mapsZeroAgeRangeToNoAgeLimit() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("no-age-limit-policy");
        item.setPolicyName("연령 제한 없는 정책");
        item.setMinAge("0");
        item.setMaxAge("0");
        item.setAgeLimitYn("Y");

        Policy policy = apiConverter.convert(item, PolicyCategory.OTHER, null,
                PolicyIncomeCondition.UNKNOWN, null, null);

        assertThat(policy.getMinAge()).isNull();
        assertThat(policy.getMaxAge()).isNull();
        assertThat(policy.getAgeLimitYn()).isFalse();
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
        verify(policySyncItemService, never()).save(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
        PolicyAiAnalysis analysis = new PolicyAiAnalysis(
                PolicyCategory.PUBLIC_RENT, 0.95, "공공임대주택 입주 정책",
                true, 0.91, "무주택 세대구성원 조건이 명시됨",
                PolicyIncomeCondition.COMPARABLE, null, 50_000_000,
                0.90, "개인 연소득 5천만원 이하");
        when(policyAiAnalyzer.analyze(item)).thenReturn(analysis);

        assertThat(service.fetchAndSaveHousingPolicies()).isEqualTo(1);
        verify(policySyncItemService).save(item, analysis);
    }

    @Test
    void skipsFailedItemAndContinuesWithNextPolicy() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://example.com/policies");

        PolicyItem failed = approvedHousingPolicy("failed-policy");
        PolicyItem succeeded = approvedHousingPolicy("succeeded-policy");
        PolicyApiResponseDTO.Pagging pagging = new PolicyApiResponseDTO.Pagging();
        pagging.setTotCount(2);
        PolicyApiResponseDTO.ResultData result = new PolicyApiResponseDTO.ResultData();
        result.setPagging(pagging);
        result.setYouthPolicyList(List.of(failed, succeeded));
        PolicyApiResponseDTO response = new PolicyApiResponseDTO();
        response.setResult(result);

        PolicyAiAnalysis analysis = new PolicyAiAnalysis(
                PolicyCategory.OTHER, 0.8, "기타 주거 정책",
                null, 0.5, "확인 필요",
                PolicyIncomeCondition.UNKNOWN, null, null,
                0.5, "확인 필요");
        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.any(java.net.URI.class),
                org.mockito.ArgumentMatchers.eq(PolicyApiResponseDTO.class))).thenReturn(response);
        when(policyAiAnalyzer.analyze(failed)).thenThrow(new RuntimeException("timeout"));
        when(policyAiAnalyzer.analyze(succeeded)).thenReturn(analysis);

        assertThat(service.fetchAndSaveHousingPolicies()).isEqualTo(1);
        verify(policySyncItemService).save(succeeded, analysis);
        verify(policySyncItemService, never()).save(
                org.mockito.ArgumentMatchers.eq(failed), org.mockito.ArgumentMatchers.any());
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
        int configuredMaxPages = (int) ReflectionTestUtils.getField(
                PolicyService.class, "MAX_PAGES");
        verify(restTemplate, times(configuredMaxPages)).getForObject(
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

    private PolicyItem approvedHousingPolicy(String policyId) {
        PolicyItem item = new PolicyItem();
        item.setPolicyId(policyId);
        item.setPolicyName(policyId);
        item.setCategory("주거");
        item.setApprovalStatusCode("44002");
        return item;
    }

    @Test
    void returnsGuestPolicyPageWithRegionsAndDeadlineInformation() {
        LocalDate today = LocalDate.now(clock);
        Region region = Region.builder()
                .regionCode("11440")
                .sigunguName("서울특별시 마포구")
                .build();
        Policy regionalPolicy = Policy.builder()
                .policyId("POLICY-1")
                .policyName("청년 월세 지원")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(false)
                .applyPeriodCode("0057001")
                .applyEndDate(today.plusDays(4))
                .build();
        Policy alwaysOpenPolicy = Policy.builder()
                .policyId("POLICY-2")
                .policyName("상시 주거 상담")
                .category(PolicyCategory.OTHER)
                .ageLimitYn(false)
                .applyPeriodCode("ALWAYS")
                .applyEndDate(null)
                .build();
        PageRequest pageRequest = PageRequest.of(0, 8, PolicySort.DEADLINE.toSort());
        when(policyRepository.findGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, today, pageRequest))
                .thenReturn(new PageImpl<>(
                        List.of(regionalPolicy, alwaysOpenPolicy), pageRequest, 9));
        when(policyRegionRepository.findAllActiveByPolicyIds(
                List.of("POLICY-1", "POLICY-2")))
                .thenReturn(List.of(PolicyRegion.builder()
                        .policy(regionalPolicy)
                        .region(region)
                        .build()));

        PolicyGuestResponseDTO.PolicyListDTO result = service.getGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, PolicySort.DEADLINE, 0, 8);

        assertThat(result.getPolicies()).hasSize(2);
        assertThat(result.getPolicies().get(0)).satisfies(item -> {
            assertThat(item.getPolicyId()).isEqualTo("POLICY-1");
            assertThat(item.getCategoryCode()).isEqualTo(PolicyCategory.MONTHLY_RENT);
            assertThat(item.getCategoryName()).isEqualTo("월세");
            assertThat(item.getRegions()).singleElement().satisfies(itemRegion -> {
                assertThat(itemRegion.getRegionCode()).isEqualTo("11440");
                assertThat(itemRegion.getRegionName()).isEqualTo("서울특별시 마포구");
            });
            assertThat(item.isNationwide()).isFalse();
            assertThat(item.getApplyPeriodCode()).isEqualTo("0057001");
            assertThat(item.getDDay()).isEqualTo(4);
        });
        assertThat(result.getPolicies().get(1)).satisfies(item -> {
            assertThat(item.getPolicyId()).isEqualTo("POLICY-2");
            assertThat(item.getRegions()).isEmpty();
            assertThat(item.isNationwide()).isTrue();
            assertThat(item.getApplyEndDate()).isNull();
            assertThat(item.getApplyPeriodCode()).isEqualTo("ALWAYS");
            assertThat(item.getDDay()).isNull();
        });
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
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
        when(favoriteRepository
                .existsByMemberMemberIdAndPolicyPolicyIdAndDeletedAtIsNull(1L, "policy-detail"))
                .thenReturn(true);

        PolicyDetailResponseDTO response =
                service.getPolicyDetailForMember("policy-detail", 1L);

        assertThat(response.policyId()).isEqualTo("policy-detail");
        assertThat(response.conditions()).hasSize(5);
        assertThat(response.isFavorite()).isTrue();
        assertThat(response.overallStatus()).isEqualTo(
                com.hyeja.domain.policy.enums.EligibilityStatus.U);
    }

    @Test
    void returnsMemberPolicyPageWithRegionsAndFavoriteStatus() {
        LocalDate today = LocalDate.now();
        Member member = Member.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("회원")
                .build();
        Region region = Region.builder()
                .regionCode("11440")
                .sigunguName("서울특별시 마포구")
                .build();
        Profile profile = Profile.builder()
                .member(member)
                .region(region)
                .birth(today.minusYears(26))
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true)
                .build();
        Policy policy = Policy.builder()
                .policyId("POLICY-1")
                .policyName("청년 월세 지원")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(true)
                .minAge(19)
                .maxAge(39)
                .applyPeriodCode("PERIOD")
                .applyEndDate(today.plusDays(4))
                .build();
        PageRequest pageRequest = PageRequest.of(0, 8, PolicySort.DEADLINE.toSort());
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(profileRepository.findById("member@example.com")).thenReturn(Optional.of(profile));
        when(policyRepository.findHousingPoliciesForMember(
                PolicyCategory.MONTHLY_RENT,
                true,
                today,
                26,
                true,
                "EMPLOYED",
                "11440",
                pageRequest
        )).thenReturn(new PageImpl<>(List.of(policy), pageRequest, 9));
        when(policyRegionRepository.findAllActiveByPolicyIds(List.of("POLICY-1")))
                .thenReturn(List.of(PolicyRegion.builder().policy(policy).region(region).build()));
        when(favoriteRepository.findActivePolicyIds(1L, List.of("POLICY-1")))
                .thenReturn(Set.of("POLICY-1"));

        PolicyListDTO result = service.getHousingPoliciesForMember(
                1L,
                PolicyCategory.MONTHLY_RENT,
                PolicySort.DEADLINE,
                true,
                0,
                8
        );

        assertThat(result.getPolicies()).singleElement().satisfies(item -> {
            assertThat(item.getPolicyId()).isEqualTo("POLICY-1");
            assertThat(item.getCategoryName()).isEqualTo("월세");
            assertThat(item.getRegions()).singleElement().satisfies(itemRegion -> {
                assertThat(itemRegion.getRegionCode()).isEqualTo("11440");
                assertThat(itemRegion.getRegionName()).isEqualTo("서울특별시 마포구");
            });
            assertThat(item.isNationwide()).isFalse();
            assertThat(item.getApplyPeriodCode()).isEqualTo("PERIOD");
            assertThat(item.getDDay()).isEqualTo(4);
            assertThat(item.isFavoriteYn()).isTrue();
        });
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
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
