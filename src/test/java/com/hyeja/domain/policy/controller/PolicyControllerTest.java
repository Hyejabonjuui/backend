package com.hyeja.domain.policy.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListItemDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyRegionItemDTO;
import com.hyeja.domain.policy.dto.PolicySearchResponseDTO;
import com.hyeja.domain.policy.enums.PolicyApplyPeriod;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.enums.EligibilityStatus;
import com.hyeja.domain.policy.service.PolicyService;
import com.hyeja.domain.policy.service.PolicySearchService;
import com.hyeja.global.exception.ExceptionAdvice;
import com.hyeja.global.exception.GeneralException;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PolicyControllerTest {

    private final PolicyService policyService = mock(PolicyService.class);
    private final PolicySearchService policySearchService = mock(PolicySearchService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new PolicyController(policyService, policySearchService))
                .setControllerAdvice(new ExceptionAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsGuestHousingPolicyPage() throws Exception {
        PolicyGuestResponseDTO.PolicyListItemDTO policy =
                PolicyGuestResponseDTO.PolicyListItemDTO.builder()
                        .policyId("POLICY-1")
                        .policyName("청년 월세 지원")
                        .categoryCodes(Set.of(PolicyCategory.MONTHLY_RENT))
                        .categoryNames(List.of("월세"))
                        .regions(List.of(PolicyGuestResponseDTO.PolicyRegionItemDTO.builder()
                                .regionCode("11440")
                                .regionName("서울특별시 마포구")
                                .build()))
                        .nationwide(false)
                        .applyEndDate(LocalDate.of(2026, 9, 30))
                        .applyPeriodCode(PolicyApplyPeriod.SPECIFIC_PERIOD)
                        .dDay(3)
                        .build();
        when(policyService.getGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, 1, 8))
                .thenReturn(PolicyGuestResponseDTO.PolicyListDTO.builder()
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
                .andExpect(jsonPath("$.result.policies[0].category_codes[0]").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.policies[0].regions[0].region_code").value("11440"))
                .andExpect(jsonPath("$.result.policies[0].apply_period_code").value("SPECIFIC_PERIOD"))
                .andExpect(jsonPath("$.result.policies[0].d_day").value(3))
                .andExpect(jsonPath("$.result.policies[0].dday").doesNotExist())
                .andExpect(jsonPath("$.result.policies[0].favorite_yn").doesNotExist());

        verify(policyService).getGuestHousingPolicies(
                PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, 1, 8);
    }

    @Test
    void usesDefaultGuestListConditions() throws Exception {
        when(policyService.getGuestHousingPolicies(null, PolicySort.DEADLINE, 0, 8))
                .thenReturn(PolicyGuestResponseDTO.PolicyListDTO.builder()
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
    void returnsBadRequestForUnknownGuestSort() throws Exception {
        mvc.perform(get("/api/policies/housing").param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsBadRequestForInvalidGuestPageConditions() throws Exception {
        mvc.perform(get("/api/policies/housing").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mvc.perform(get("/api/policies/housing").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mvc.perform(get("/api/policies/housing").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsHousingPoliciesForMember() throws Exception {
        PolicyListItemDTO policy = PolicyListItemDTO.builder()
                .policyId("POLICY-1")
                .policyName("청년 월세 지원")
                .categoryCodes(java.util.Set.of(PolicyCategory.MONTHLY_RENT))
                .categoryNames(List.of("월세"))
                .regions(List.of(PolicyRegionItemDTO.builder()
                        .regionCode("11440")
                        .regionName("서울특별시 마포구")
                        .build()))
                .nationwide(false)
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyPeriodCode(com.hyeja.domain.policy.enums.PolicyApplyPeriod.SPECIFIC_PERIOD)
                .dDay(4)
                .favoriteYn(true)
                .build();
        when(policyService.getHousingPoliciesForMember(
                1L, PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, true, 0, 8))
                .thenReturn(PolicyListDTO.builder()
                        .policies(List.of(policy))
                        .page(0)
                        .size(8)
                        .totalElements(9)
                        .totalPages(2)
                        .hasNext(true)
                        .build());

        mvc.perform(get("/api/policies/housing/me")
                        .param("category", "MONTHLY_RENT")
                        .param("sort", "VIEW_COUNT")
                        .param("onlyEligible", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.totalElements").value(9))
                .andExpect(jsonPath("$.result.policies[0].policy_id").value("POLICY-1"))
                .andExpect(jsonPath("$.result.policies[0].category_codes[0]").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.policies[0].regions[0].region_code").value("11440"))
                .andExpect(jsonPath("$.result.policies[0].apply_period_code").value("SPECIFIC_PERIOD"))
                .andExpect(jsonPath("$.result.policies[0].d_day").value(4))
                .andExpect(jsonPath("$.result.policies[0].dday").doesNotExist())
                .andExpect(jsonPath("$.result.policies[0].favorite_yn").value(true));

        verify(policyService).getHousingPoliciesForMember(
                1L, PolicyCategory.MONTHLY_RENT, PolicySort.VIEW_COUNT, true, 0, 8);
    }

    @Test
    void usesDefaultListConditions() throws Exception {
        when(policyService.getHousingPoliciesForMember(
                1L, null, PolicySort.DEADLINE, false, 0, 8))
                .thenReturn(PolicyListDTO.builder()
                        .policies(List.of())
                        .page(0)
                        .size(8)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .build());

        mvc.perform(get("/api/policies/housing/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policies").isEmpty());

        verify(policyService).getHousingPoliciesForMember(
                1L, null, PolicySort.DEADLINE, false, 0, 8);
    }

    @Test
    void returnsBadRequestForUnknownSort() throws Exception {
        mvc.perform(get("/api/policies/housing/me")
                        .param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsBadRequestForTooLargeMemberPageSize() throws Exception {
        mvc.perform(get("/api/policies/housing/me").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void returnsMemberPolicySearchResult() throws Exception {
        PolicySearchResponseDTO result = new PolicySearchResponseDTO(
                List.of(new PolicySearchResponseDTO.PolicySearchItemDTO(
                        "POLICY-1", "청년 월세 지원", Set.of(PolicyCategory.MONTHLY_RENT),
                        LocalDate.of(2026, 9, 30), PolicyApplyPeriod.SPECIFIC_PERIOD,
                        true, "서울에 거주하고 무주택이라 신청할 수 있어요.",
                        new PolicySearchResponseDTO.PolicyEligibilityStatusDTO(
                                EligibilityStatus.ABLE, EligibilityStatus.ABLE,
                                EligibilityStatus.ABLE, EligibilityStatus.ABLE,
                                EligibilityStatus.ABLE))),
                List.of(), List.of());
        when(policySearchService.search(1L, "#월세")).thenReturn(result);

        mvc.perform(get("/api/policies/search").param("query", "#월세"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.approved[0].policyId").value("POLICY-1"))
                .andExpect(jsonPath("$.result.approved[0].categories[0]").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.approved[0].isFavorite").value(true))
                .andExpect(jsonPath("$.result.approved[0].status.region").value("ABLE"))
                .andExpect(jsonPath("$.result.underReview").isEmpty())
                .andExpect(jsonPath("$.result.declined").isEmpty());

        verify(policySearchService).search(1L, "#월세");
    }

    @Test
    void rejectsPolicySearchQueryLongerThanTwoHundredCharacters() throws Exception {
        String query = "가".repeat(201);
        when(policySearchService.search(1L, query))
                .thenThrow(new GeneralException(ErrorStatus.POLICY_SEARCH_QUERY_TOO_LONG));

        mvc.perform(get("/api/policies/search").param("query", query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POLICY_SEARCH_007"))
                .andExpect(jsonPath("$.message").value("검색어는 200자 이하여야 합니다."))
                .andExpect(jsonPath("$.result").isEmpty());

        verify(policySearchService).search(1L, query);
    }
}
