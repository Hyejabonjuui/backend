package com.hyeja.domain.cardnews.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import com.hyeja.domain.cardnews.dto.CardNewsDetailResponseDTO;
import com.hyeja.domain.cardnews.dto.CardNewsResponseDTO;
import com.hyeja.domain.cardnews.dto.MemberCardNewsResponseDTO;
import com.hyeja.domain.cardnews.service.CardNewsService;
import com.hyeja.global.apiPayload.ApiResponse;
import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.exception.GeneralException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (SUCCESS_001)"
            )
    })
    @GetMapping("/card-news/guest")
    public ApiResponse<List<CardNewsResponseDTO>> getGuestCardNews(

    ) {
        List<CardNewsResponseDTO> result = cardNewsService.getGuestCardNews();
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "카드뉴스 팝업 상세 조회",
            description = "비로그인도 조회할 수 있습니다. 토큰을 보냈는데 무효하면(만료·위조·로그아웃, 탈퇴할 때 쓴 토큰) 비로그인으로 보지 않고 401입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "보낸 토큰이 무효함 (COMMON_002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드뉴스 없음 (CARD_NEWS_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/card-detail/{policyId}")
    public ApiResponse<CardNewsDetailResponseDTO> getCardNewsDetail(
            @PathVariable String policyId,
            @AuthenticationPrincipal Long memberId,
            @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        // 비로그인도 볼 수 있는 주소라 인증 필터가 무효한 토큰을 막지 않습니다. 토큰을 보냈는데 무효하면
        // 조용히 비로그인 화면을 주지 않고 다른 API처럼 401로 알려, 프론트가 재로그인을 안내하게 합니다(정책 상세와 같음).
        if (memberId == null && authorization != null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        return ApiResponse.onSuccess(cardNewsService.getCardNewsDetail(policyId, memberId));
    }

    @Operation(
            summary = "로그인 회원 맞춤 홈 카드뉴스 조회",
            description = "회원 거주지 기준으로 마감이 가까운 카드뉴스를 최대 4건 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (SUCCESS_001)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "토큰 없음·무효 (COMMON_002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001) / 조건 없음 (PROFILE_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/card-news")
    public ApiResponse<List<MemberCardNewsResponseDTO>> getMemberCardNews(
            @AuthenticationPrincipal Long memberId) {
        return ApiResponse.onSuccess(cardNewsService.getMemberCardNews(memberId));
    }
}
