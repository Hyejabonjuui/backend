package com.hyeja.domain.favorite.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FavoriteControllerTest {

    private final FavoriteService favoriteService = mock(FavoriteService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favoriteService))
                .setControllerAdvice(new ExceptionAdvice())
                .build();
    }

    @Test
    void returnsMyFavoritePolicies() throws Exception {
        FavoriteItemDTO favorite = FavoriteItemDTO.builder()
                .favoriteId(10L)
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .categoryCode(PolicyCategory.MONTHLY_RENT)
                .categoryName("월세")
                .supportContent("월세를 지원합니다.")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        when(favoriteService.getMyFavorites(1L, 0, 8)).thenReturn(FavoriteListDTO.builder()
                .favorites(List.of(favorite))
                .page(0)
                .size(8)
                .totalElements(9)
                .totalPages(2)
                .hasNext(true)
                .build());

        mvc.perform(get("/api/favorite").param("memberId", "1"))
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
                .andExpect(jsonPath("$.result.favorites[0].category_code").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.favorites[0].category_name").value("월세"))
                .andExpect(jsonPath("$.result.favorites[0].support_content").value("월세를 지원합니다."))
                .andExpect(jsonPath("$.result.favorites[0].apply_end_date").value("2026-09-30"))
                .andExpect(jsonPath("$.result.favorites[0].apply_url").value("https://example.com/apply"))
                .andExpect(jsonPath("$.result.favorites[0].created_at").value("2026-09-24T10:30:00"));

        verify(favoriteService).getMyFavorites(1L, 0, 8);
    }

    @Test
    void returnsNotFoundWhenMemberDoesNotExist() throws Exception {
        when(favoriteService.getMyFavorites(99L, 0, 8))
                .thenThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        mvc.perform(get("/api/favorite").param("memberId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void returnsBadRequestWithoutMemberId() throws Exception {
        mvc.perform(get("/api/favorite"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void createsFavoritePolicy() throws Exception {
        FavoriteItemDTO response = FavoriteItemDTO.builder()
                .favoriteId(10L)
                .policyId("policy-1")
                .policyName("청년 월세 지원")
                .categoryCode(PolicyCategory.MONTHLY_RENT)
                .categoryName("월세")
                .supportContent("월세를 지원합니다.")
                .applyEndDate(LocalDate.of(2026, 9, 30))
                .applyUrl("https://example.com/apply")
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();
        when(favoriteService.createFavorite(1L, "policy-1")).thenReturn(response);

        mvc.perform(post("/api/favorite/{policyId}", "policy-1")
                        .param("memberId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS_002"))
                .andExpect(jsonPath("$.message").value("생성되었습니다."))
                .andExpect(jsonPath("$.result.favorite_id").value(10))
                .andExpect(jsonPath("$.result.policy_id").value("policy-1"))
                .andExpect(jsonPath("$.result.policy_name").value("청년 월세 지원"))
                .andExpect(jsonPath("$.result.category_code").value("MONTHLY_RENT"))
                .andExpect(jsonPath("$.result.category_name").value("월세"))
                .andExpect(jsonPath("$.result.apply_end_date").value("2026-09-30"));

        verify(favoriteService).createFavorite(1L, "policy-1");
    }

    @Test
    void returnsConflictWhenFavoriteAlreadyExists() throws Exception {
        when(favoriteService.createFavorite(1L, "policy-1"))
                .thenThrow(new GeneralException(ErrorStatus.FAVORITE_ALREADY_EXISTS));

        mvc.perform(post("/api/favorite/{policyId}", "policy-1")
                        .param("memberId", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void returnsNotFoundWhenPolicyDoesNotExist() throws Exception {
        when(favoriteService.createFavorite(1L, "missing-policy"))
                .thenThrow(new GeneralException(ErrorStatus.POLICY_NOT_FOUND));

        mvc.perform(post("/api/favorite/{policyId}", "missing-policy")
                        .param("memberId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_001"))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }

    @Test
    void createReturnsBadRequestWithoutMemberId() throws Exception {
        mvc.perform(post("/api/favorite/{policyId}", "policy-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
