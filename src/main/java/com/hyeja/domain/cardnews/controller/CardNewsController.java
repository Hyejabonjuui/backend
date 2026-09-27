package com.hyeja.domain.cardnews.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO;
import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.service.CardNewsService;
import com.hyeja.global.apiPayload.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "카드뉴스", description = "카드뉴스 API")
@RestController 
@RequestMapping("/api/policies")
@RequiredArgsConstructor 
public class CardNewsController {
    private final CardNewsService cardNewsService;

    @Operation(
            summary = "비회원 카드뉴스 조회",
            description = "비회원 메인 화면에 노출할 최신 카드뉴스를 조회합니다."
    )
    @GetMapping("/card-news/guest")
    public ApiResponse<List<CardNewsResponseDTO>> getGuestCardNews(

    ) {
        List<CardNewsResponseDTO> result = cardNewsService.getGuestCardNews();
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "카드뉴스 팝업 상세 조회")
    @GetMapping("/card-detail/{cardNewsId}")
    public ApiResponse<CardNewsDetailResponseDTO> getCardNewsDetail(
            @PathVariable Long cardNewsId,
            @AuthenticationPrincipal Long memberId) {
        return ApiResponse.onSuccess(cardNewsService.getCardNewsDetail(cardNewsId, memberId));
    }
}
