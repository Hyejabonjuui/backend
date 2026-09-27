package com.hyeja.domain.cardnews.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO;
import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO.CardDTO;
import com.hyeja.domain.cardnews.service.CardNewsService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CardNewsControllerTest {
    private final CardNewsService cardNewsService = mock(CardNewsService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new CardNewsController(cardNewsService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCardNewsPopupDetail() throws Exception {
        when(cardNewsService.getCardNewsDetail(10L, 1L)).thenReturn(
                new CardNewsDetailResponseDTO(
                        "policy-1", "월세", 5, true, true,
                        "https://example.com/apply",
                        List.of(new CardDTO(10L, 2L, null,
                                List.of("만 19~34세", "전국"), "신청 대상"))));

        mvc.perform(get("/api/policies/card-detail/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.policyId").value("policy-1"))
                .andExpect(jsonPath("$.result.categoryLabel").value("월세"))
                .andExpect(jsonPath("$.result.isAuthenticated").value(true))
                .andExpect(jsonPath("$.result.isFavorite").value(true))
                .andExpect(jsonPath("$.result.cards[0].badges[0]").value("만 19~34세"));

        verify(cardNewsService).getCardNewsDetail(10L, 1L);
    }
}
