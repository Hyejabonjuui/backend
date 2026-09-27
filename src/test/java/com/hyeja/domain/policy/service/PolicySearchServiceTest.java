package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.dto.PolicySearchResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.EligibilityConditionType;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.enums.PolicyApplyPeriod;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.SearchIntent;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.enums.HousingType;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicySearchServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final PolicyRegionRepository policyRegionRepository = mock(PolicyRegionRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final ProfileService profileService = mock(ProfileService.class);
    private final PolicyEligibilityEvaluator eligibilityEvaluator = mock(PolicyEligibilityEvaluator.class);
    private final PolicySearchAiAnalyzer aiAnalyzer = mock(PolicySearchAiAnalyzer.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-27T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final PolicySearchService service = new PolicySearchService(
            policyRepository, policyRegionRepository, favoriteRepository, profileService,
            eligibilityEvaluator, aiAnalyzer, clock);

    private Profile profile;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .email("member@example.com")
                .password("encoded")
                .nickname("회원")
                .build();
        Region region = Region.builder()
                .regionCode("11440")
                .sigunguName("서울특별시 마포구")
                .build();
        profile = Profile.builder()
                .member(member)
                .region(region)
                .birth(LocalDate.of(2000, 1, 1))
                .employmentCode(EmploymentStatus.EMPLOYED)
                .houselessYn(true)
                .housingType(HousingType.MONTHLY_RENT)
                .build();
        when(profileService.getActiveProfile(1L)).thenReturn(profile);
    }

    @Test
    void mapsHashtagWithoutAiAndGroupsAtMostTwentyPolicies() {
        Policy approved = policy("approved");
        Policy review = policy("review");
        Policy declined = policy("declined");
        when(policyRepository.searchActivePolicies(
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq("11440"),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 9, 27)),
                any())).thenReturn(List.of(approved, review, declined));
        when(policyRegionRepository.findAllActiveByPolicyIds(anyList())).thenReturn(List.of());
        when(favoriteRepository.findActivePolicyIds(1L, List.of("approved", "review", "declined")))
                .thenReturn(Set.of("approved"));
        when(eligibilityEvaluator.evaluate(approved, profile, List.of()))
                .thenReturn(conditions(EligibilityStatus.ABLE));
        when(eligibilityEvaluator.evaluate(review, profile, List.of()))
                .thenReturn(conditions(EligibilityStatus.UNKNOWN));
        when(eligibilityEvaluator.evaluate(declined, profile, List.of()))
                .thenReturn(conditions(EligibilityStatus.DISABLE));
        when(aiAnalyzer.generateReasons(anyList())).thenReturn(Map.of(
                "approved", "서울에 거주하고 무주택이라 신청할 수 있어요.",
                "review", "소득 정보를 입력하면 정확히 알려드려요.",
                "declined", "만 34세까지 신청할 수 있지만 현재 만 36세예요."));

        PolicySearchResponseDTO response = service.search(1L, " #월세 ");

        assertThat(response.approved()).singleElement().satisfies(item -> {
            assertThat(item.policyId()).isEqualTo("approved");
            assertThat(item.isFavorite()).isTrue();
            assertThat(item.categories()).containsExactly(PolicyCategory.MONTHLY_RENT);
        });
        assertThat(response.underReview()).extracting(item -> item.policyId())
                .containsExactly("review");
        assertThat(response.declined()).extracting(item -> item.policyId())
                .containsExactly("declined");
        assertThat(response.approved().size() + response.underReview().size()
                + response.declined().size()).isLessThanOrEqualTo(20);
        verify(aiAnalyzer, never()).analyzeIntent(any());
    }

    @Test
    void throwsDedicatedErrorWhenNoCandidateExists() {
        when(policyRepository.searchActivePolicies(
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any(Boolean.class),
                any(Boolean.class), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.search(1L, "#전세"))
                .isInstanceOfSatisfying(GeneralException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus.POLICY_SEARCH_EMPTY));
    }

    @Test
    void rejectsNonHousingNaturalLanguageQuestion() {
        when(aiAnalyzer.analyzeIntent("취업 지원금 알려줘"))
                .thenReturn(new SearchIntent(false, Set.of()));

        assertThatThrownBy(() -> service.search(1L, "취업 지원금 알려줘"))
                .isInstanceOfSatisfying(GeneralException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus.POLICY_SEARCH_NOT_HOUSING));
        verify(policyRepository, never()).searchActivePolicies(
                any(Boolean.class), any(Boolean.class), any(Boolean.class), any(Boolean.class),
                any(Boolean.class), any(), any(), any());
    }

    private Policy policy(String id) {
        return Policy.builder()
                .policyId(id)
                .policyName("청년 월세 지원")
                .categories(Set.of(PolicyCategory.MONTHLY_RENT))
                .ageLimitYn(false)
                .applyPeriodCode(PolicyApplyPeriod.ALWAYS)
                .activeYn(true)
                .build();
    }

    private List<ConditionResultDTO> conditions(EligibilityStatus varyingStatus) {
        return List.of(
                condition(EligibilityConditionType.AGE, varyingStatus),
                condition(EligibilityConditionType.REGION, EligibilityStatus.ABLE),
                condition(EligibilityConditionType.INCOME, EligibilityStatus.ABLE),
                condition(EligibilityConditionType.EMPLOYMENT, EligibilityStatus.ABLE),
                condition(EligibilityConditionType.HOUSELESS, EligibilityStatus.ABLE));
    }

    private ConditionResultDTO condition(EligibilityConditionType type, EligibilityStatus status) {
        return new ConditionResultDTO(type, status, "정책 조건", "회원 정보");
    }
}
