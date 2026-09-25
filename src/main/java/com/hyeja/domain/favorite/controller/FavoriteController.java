package com.hyeja.domain.favorite.controller;

import com.hyeja.domain.favorite.dto.FavoriteResponseDTO.FavoriteListDTO;
import com.hyeja.domain.favorite.service.FavoriteService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관심 정책", description = "회원 관심 정책 API")
@Validated
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(
            summary = "관심 정책 목록 조회",
            description = "회원이 관심 정책으로 등록한 정책을 최근 등록순으로 8개씩 조회합니다. "
                    + "더보기 요청 시 page 값을 1씩 증가시키며, 관심 정책이 없으면 빈 목록을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "관심 정책 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "회원 ID 누락 (COMMON_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "없거나 탈퇴한 회원 (MEMBER_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/me/favorites")
    public ResponseEntity<ApiResponse<FavoriteListDTO>> getMyFavorites(
            @Parameter(
                    name = "memberId",
                    description = "조회할 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            )
            @RequestParam(name = "memberId") Long memberId,
            @Parameter(
                    name = "page",
                    description = "페이지 번호(0부터 시작)",
                    in = ParameterIn.QUERY,
                    example = "0"
            )
            @RequestParam(name = "page", defaultValue = "0")
            @PositiveOrZero(message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @Parameter(
                    name = "size",
                    description = "페이지당 관심 정책 개수",
                    in = ParameterIn.QUERY,
                    example = "8"
            )
            @RequestParam(name = "size", defaultValue = "8")
            @Positive(message = "페이지 크기는 양수여야 합니다.") int size
    ) {
        FavoriteListDTO result = favoriteService.getMyFavorites(memberId, page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
