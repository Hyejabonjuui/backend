package com.hyeja.domain.policy.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyListItemDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyRegionItemDTO;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.global.exception.ExceptionAdvice;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PolicyControllerTest {

    private final PolicyService policyService = mock(PolicyService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PolicyController(policyService))
                .setControllerAdvice(new ExceptionAdvice())
                .build();
    }

    @Test
    void returnsGuestHousingPolicyPage() throws Exception {
        PolicyListItemDTO policy = PolicyListItemDTO.builder()
                .policyId("POLICY-1")
                .policyName("청년 월세 지원")
                .categoryCode(PolicyCategory.MONTHLY_RENT)
                .categoryName("월세")
                .regions(List.of(PolicyRegionItemDTO.builder()
                        .regionCode("11440")
                        .regionName("서울특별시 마포구")
                        .build()))
                .nationwide(false)
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .dDay(3)
                .build();
        when(policyService.getGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, 1, 8))
                .thenReturn(PolicyListDTO.builder()
                        .policies(List.of(policy))
                        .page(1)
                        .size(8)
                        .totalElements(9)
                        .totalPages(2)
                        .hasNext(false)
                        .build());

        mvc.perform(get("/api/policies/housing")
                        .param("category", "MONTHLY_RENT")
                        .param("sort", "VIEW_COUNT")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.size").value(8))
                .andExpect(jsonPath("$.result.totalElements").value(9))
                .andExpect(jsonPath("$.result.policies[0].policy_id").value("POLICY-1"))
                .andExpect(jsonPath("$.result.policies[0].category_code").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.policies[0].regions[0].region_code").value("11440"))
                .andExpect(jsonPath("$.result.policies[0].d_day").value(3))
                .andExpect(jsonPath("$.result.policies[0].favorite_yn").doesNotExist());

        verify(policyService).getGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, 1, 8);
    }

    @Test
    void usesDefaultGuestListConditions() throws Exception {
        when(policyService.getGuestHousingPolicies(null, PolicySort.DEADLINE, 0, 8))
                .thenReturn(PolicyListDTO.builder()
                        .policies(List.of())
                        .page(0)
                        .size(8)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .build());

        mvc.perform(get("/api/policies/housing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policies").isEmpty())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(8));

        verify(policyService).getGuestHousingPolicies(null, PolicySort.DEADLINE, 0, 8);
    }

    @Test
    void returnsBadRequestForUnknownSort() throws Exception {
        mvc.perform(get("/api/policies/housing").param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsBadRequestForInvalidPageConditions() throws Exception {
        mvc.perform(get("/api/policies/housing").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mvc.perform(get("/api/policies/housing").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
