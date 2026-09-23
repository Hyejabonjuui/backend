package com.hyeja.ctrl.cardnews;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyeja.domain.cardnews.dto.CardNewsRequestDTO;
import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.global.apiPayload.ApiResponse;
import com.hyeja.service.cardnews.CardNewsService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/policies/card-news")
@RequiredArgsConstructor 
public class CardNewsController {
    private final CardNewsService cardNewsService;

    @GetMapping("/guest")
    public ResponseEntity<ApiResponse<List<CardNewsResponseDTO>>> getGuestCardNews(
            @ModelAttribute CardNewsRequestDTO requestDto
    ) {
        List<CardNewsResponseDTO> result = cardNewsService.getGuestCardNews(requestDto);
        
        // 만약 팀 내 ApiResponse 구현체 생성 메서드가 success가 아니라면 프로젝트 내 공통 응답 규격(예: onSuccess 등)으로 변경해주세요.
        return ResponseEntity.ok(ApiResponse.onSuccess(result)); 
    }
}
