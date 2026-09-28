package com.hyeja.domain.term.controller;

import com.hyeja.domain.term.dto.TermResponseDTO;
import com.hyeja.domain.term.dto.TermDetailResponseDTO;
import com.hyeja.domain.term.service.TermService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "용어", description = "정책 용어 풀이 API")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @Operation(
            summary = "용어 풀이 목록 조회",
            description = "정책 화면에서 어려운 용어를 설명하는 데 사용할 전체 용어 풀이를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "용어 풀이 목록 조회 성공"
            )
    })
    @GetMapping("")
    public ApiResponse<List<TermResponseDTO>> getTerms() {
        return ApiResponse.onSuccess(termService.getTerms());
    }

    @Operation(summary = "용어 상세 조회", description = "선택한 정책 용어의 쉬운 설명을 반환합니다.")
    @GetMapping("/{termId}")
    public ApiResponse<TermDetailResponseDTO> getTerm(
            @PathVariable("termId") Long termId) {
        return ApiResponse.onSuccess(termService.getTerm(termId));
    }
}
