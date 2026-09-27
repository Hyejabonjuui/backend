package com.hyeja.domain.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "TermResponseDTO", description = "정책 용어 풀이")
public class TermResponseDTO {

    @Schema(description = "용어 ID", example = "1")
    private Integer termId;

    @Schema(description = "정책 용어", example = "중위소득")
    private String term;

    @Schema(description = "쉬운 설명", example = "전체 가구를 소득 순서로 세웠을 때 가운데 가구의 소득입니다.")
    private String easyDescription;

    @Schema(description = "예시 문장", example = "중위소득 60% 이하인 가구", nullable = true)
    private String example;
}
