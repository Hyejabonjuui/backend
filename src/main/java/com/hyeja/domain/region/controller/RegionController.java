package com.hyeja.domain.region.controller;

import com.hyeja.domain.region.dto.RegionResponseDTO.SidoDTO;
import com.hyeja.domain.region.service.RegionService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "지역", description = "거주지 선택용 지역 API")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // 시군구 목록 (회원가입 S-04·내 조건 수정 S-08의 거주지 2단 드롭다운) — 예: GET /api/regions
    // 비로그인도 호출할 수 있습니다(가입 전 화면에서 씀).
    @Operation(
            summary = "시군구 목록 조회",
            description = "전체 시·군·구를 시·도별로 묶어 코드 순으로 한 번에 반환합니다. "
                    + "선택한 시·군·구의 regionCode를 회원가입·내 조건 수정 요청에 보냅니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "시군구 목록 조회 성공"
            )
    })
    @GetMapping("")
    public ApiResponse<List<SidoDTO>> getRegions() {
        return ApiResponse.onSuccess(regionService.getRegions());
    }
}
