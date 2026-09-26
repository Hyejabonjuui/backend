package com.hyeja.domain.cardnews.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.service.CardNewsService;
import com.hyeja.global.apiPayload.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "카드뉴스", description = "카드뉴스 API")
@RestController 
@RequestMapping("/api/policies/card-news")
@RequiredArgsConstructor 
public class CardNewsController {
    private final CardNewsService cardNewsService;

    @Operation(
            summary = "비회원 카드뉴스 조회",
            description = "비회원 메인 화면에 노출할 최신 카드뉴스를 조회합니다."
    )
    @GetMapping("/guest")
    public ApiResponse<List<CardNewsResponseDTO>> getGuestCardNews(

    ) {
        List<CardNewsResponseDTO> result = cardNewsService.getGuestCardNews();
        return ApiResponse.onSuccess(result);
    }
}
