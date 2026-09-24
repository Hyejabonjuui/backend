package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepository, cardNewsRepository, restTemplate);
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
