package com.hyeja.domain.cardnews.ctrl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.service.CardNewsService;
import com.hyeja.global.apiPayload.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/policies/card-news")
@RequiredArgsConstructor 
public class CardNewsController {
    private final CardNewsService cardNewsService;

    @GetMapping("/guest")
    public ApiResponse<List<CardNewsResponseDTO>> getGuestCardNews(

    ) {
        List<CardNewsResponseDTO> result = cardNewsService.getGuestCardNews();
        return ApiResponse.onSuccess(result);
    }
}
