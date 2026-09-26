package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.enums.EmploymentStatus;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.domain.member.entity.Member;
import com.hyeja.domain.region.entity.Region;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private CardNewsRepository cardNewsRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProfileService profileService;

    @Mock
    private PolicyRegionRepository policyRegionRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(
                policyRepository,
                cardNewsRepository,
                restTemplate,
                profileService,
                policyRegionRepository,
                favoriteRepository
        );
        ReflectionTestUtils.setField(policyService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(policyService, "apiUrl", "https://example.com/policies");
    }

    @Test
    void savesHousingPolicyWithFallbackCategory() {
        PolicyApiResponseDTO.PolicyItem item = policyItem("주거");
        when(restTemplate.getForObject(any(URI.class), eq(PolicyApiResponseDTO.class)))
                .thenReturn(responseOf(item));
        when(cardNewsRepository.existsByPolicy_PolicyIdAndCardNo("POLICY-1", 1L))
                .thenReturn(true);

        policyService.fetchAndSaveHousingPolicies();

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(captor.capture());
        Policy saved = captor.getValue();
        assertThat(saved.getPolicyId()).isEqualTo("POLICY-1");
        assertThat(saved.getCategory()).isEqualTo(PolicyCategory.OTHER);
        assertThat(saved.getApplyEndDate()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void ignoresPolicyOutsideHousingCategory() {
        when(restTemplate.getForObject(any(URI.class), eq(PolicyApiResponseDTO.class)))
                .thenReturn(responseOf(policyItem("일자리")));

        policyService.fetchAndSaveHousingPolicies();

        verifyNoInteractions(policyRepository, cardNewsRepository);
    }

    @Test
    void returnsEveryStoredHousingPolicyInDeadlineOrder() {
        List<Policy> policies = List.of(Policy.builder()
                .policyId("POLICY-1")
                .policyName("주거 정책")
                .category(PolicyCategory.MONTHLY_RENT)
                .ageLimitYn(false)
                .applyPeriodCode("003")
                .build());
        when(policyRepository.findAllByOrderByApplyEndDateAsc()).thenReturn(policies);

        assertThat(policyService.getHousingPolicies()).isSameAs(policies);
        verify(policyRepository).findAllByOrderByApplyEndDateAsc();
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
        when(profileService.getActiveProfile(1L)).thenReturn(profile);
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

        PolicyListDTO result = policyService.getHousingPoliciesForMember(
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
            assertThat(item.getDDay()).isEqualTo(4);
            assertThat(item.isFavoriteYn()).isTrue();
        });
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
    }

    private PolicyApiResponseDTO responseOf(PolicyApiResponseDTO.PolicyItem item) {
        PolicyApiResponseDTO.ResultData result = new PolicyApiResponseDTO.ResultData();
        result.setYouthPolicyList(List.of(item));
        PolicyApiResponseDTO response = new PolicyApiResponseDTO();
        response.setResult(result);
        return response;
    }

    private PolicyApiResponseDTO.PolicyItem policyItem(String category) {
        PolicyApiResponseDTO.PolicyItem item = new PolicyApiResponseDTO.PolicyItem();
        item.setPolicyId("POLICY-1");
        item.setPolicyName("청년 주거 정책");
        item.setCategory(category);
        item.setDescription("정책 설명");
        item.setSupportContent("지원 내용");
        item.setApplyUrl("https://example.com/apply");
        item.setApplyYmd("20260923 ~ 20260928");
        return item;
    }
}
