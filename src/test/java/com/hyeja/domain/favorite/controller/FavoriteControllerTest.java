package com.hyeja.domain.favorite.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteItemDTO;
import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.service.FavoriteService;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.ExceptionAdvice;
import com.hyeja.global.exception.GeneralException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FavoriteControllerTest {

    private final FavoriteService favoriteService = mock(FavoriteService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favoriteService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new ExceptionAdvice())
                .build();
        loginAs(1L);
    }

    // 토큰 대신 "1번 회원으로 로그인한 상태"를 직접 만들어, @AuthenticationPrincipal Long memberId에 1이 들어가게 합니다.
    // (실제 토큰 검증은 SecurityTest에서 확인합니다.)
    private static void loginAs(Long memberId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, "access-token", List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsMyFavoritePolicies() throws Exception {
        FavoriteItemDTO favorite = FavoriteItemDTO.builder()
                .favoriteId(10L)
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .categoryCodes(java.util.Set.of(PolicyCategory.MONTHLY_RENT))
                .categoryNames(java.util.List.of("월세"))
                .supportContent("월세를 지원합니다.")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyPeriodCode(com.hyeja.domain.policy.enums.PolicyApplyPeriod.CLOSED)
                .applyUrl("https://example.com/apply")
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        when(favoriteService.getMyFavorites(1L, null, 0, 8)).thenReturn(FavoriteListDTO.builder()
                .favorites(List.of(favorite))
                .page(0)
                .size(8)
                .totalElements(9)
                .totalPages(2)
                .hasNext(true)
                .build());

        mvc.perform(get("/api/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(8))
                .andExpect(jsonPath("$.result.totalElements").value(9))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.favorites[0].favorite_id").value(10))
                .andExpect(jsonPath("$.result.favorites[0].policy_id").value("policy-1"))
                .andExpect(jsonPath("$.result.favorites[0].policy_name").value("청년 월세 지원"))
                .andExpect(jsonPath("$.result.favorites[0].category_codes[0]").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.favorites[0].category_names[0]").value("월세"))
                .andExpect(jsonPath("$.result.favorites[0].support_content").value("월세를 지원합니다."))
                .andExpect(jsonPath("$.result.favorites[0].apply_end_date").value("2026-09-30"))
                .andExpect(jsonPath("$.result.favorites[0].apply_period_code").value("CLOSED"))
                .andExpect(jsonPath("$.result.favorites[0].apply_url").value("https://example.com/apply"))
                .andExpect(jsonPath("$.result.favorites[0].created_at").value("2026-09-24T10:30:00"));

        verify(favoriteService).getMyFavorites(1L, null, 0, 8);
    }

    @Test
    void searchesMyFavoritePoliciesByKeyword() throws Exception {
        when(favoriteService.getMyFavorites(1L, "월세", 0, 8))
                .thenReturn(FavoriteListDTO.builder()
                        .favorites(List.of())
                        .page(0)
                        .size(8)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .build());

        mvc.perform(get("/api/favorite")
                        .param("keyword", "월세"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result.favorites").isEmpty())
                .andExpect(jsonPath("$.result.totalElements").value(0));

        verify(favoriteService).getMyFavorites(1L, "월세", 0, 8);
    }

    @Test
    void returnsNotFoundWhenMemberDoesNotExist() throws Exception {
        loginAs(99L);
        when(favoriteService.getMyFavorites(99L, null, 0, 8))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        mvc.perform(get("/api/favorite"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void createsFavoritePolicy() throws Exception {
        FavoriteItemDTO response = FavoriteItemDTO.builder()
                .favoriteId(10L)
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .categoryCodes(java.util.Set.of(PolicyCategory.MONTHLY_RENT))
                .categoryNames(java.util.List.of("월세"))
                .supportContent("월세를 지원합니다.")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyPeriodCode(com.hyeja.domain.policy.enums.PolicyApplyPeriod.CLOSED)
                .applyUrl("https://example.com/apply")
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        when(favoriteService.createFavorite(1L, "policy-1")).thenReturn(response);

        mvc.perform(post("/api/favorite/{policyId}", "policy-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.message").value("성공입니다."))
                .andExpect(jsonPath("$.result.favorite_id").value(10))
                .andExpect(jsonPath("$.result.policy_id").value("policy-1"))
                .andExpect(jsonPath("$.result.policy_name").value("청년 월세 지원"))
                .andExpect(jsonPath("$.result.category_codes[0]").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.category_names[0]").value("월세"))
                .andExpect(jsonPath("$.result.apply_end_date").value("2026-09-30"))
                .andExpect(jsonPath("$.result.apply_period_code").value("CLOSED"));

        verify(favoriteService).createFavorite(1L, "policy-1");
    }

    @Test
    void returnsConflictWhenFavoriteAlreadyExists() throws Exception {
        when(favoriteService.createFavorite(1L, "policy-1"))
                .thenThrow(new GeneralException(ErrorStatus.FAVORITE_ALREADY_EXISTS));

        mvc.perform(post("/api/favorite/{policyId}", "policy-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void returnsNotFoundWhenPolicyDoesNotExist() throws Exception {
        when(favoriteService.createFavorite(1L, "missing-policy"))
                .thenThrow(new GeneralException(ErrorStatus.POLICY_NOT_FOUND));

        mvc.perform(post("/api/favorite/{policyId}", "missing-policy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void deletesFavoritePolicy() throws Exception {
        mvc.perform(delete("/api/favorite/{policyId}", "policy-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));

        verify(favoriteService).deleteFavorite(1L, "policy-1");
    }

    @Test
    void deleteReturnsNotFoundWhenFavoriteDoesNotExist() throws Exception {
        doThrow(new GeneralException(ErrorStatus.FAVORITE_NOT_FOUND))
                .when(favoriteService).deleteFavorite(1L, "policy-1");

        mvc.perform(delete("/api/favorite/{policyId}", "policy-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE_002"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }
}
