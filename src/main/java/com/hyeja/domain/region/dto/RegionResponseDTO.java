package com.hyeja.domain.region.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

// 거주지 2단 드롭다운(시·도 → 시·군·구)용 지역 목록 응답입니다.
public final class RegionResponseDTO {

    private RegionResponseDTO() {
    }

    @Getter
    @Builder
    @Schema(name = "SidoDTO", description = "시·도와 그 안의 시·군·구 목록")
    public static class SidoDTO {

        @Schema(description = "시·도 코드 (시군구코드 앞 2자리)", example = "11")
        private String sidoCode;

        @Schema(description = "시·도 이름", example = "서울특별시")
        private String sidoName;

        @Schema(description = "시·군·구 목록 (코드 순)")
        private List<SigunguDTO> sigungu;
    }

    @Getter
    @Builder
    @Schema(name = "SigunguDTO", description = "시·군·구")
    public static class SigunguDTO {

        @Schema(description = "시군구코드 5자리 (회원가입·조건 수정의 regionCode로 보냄)", example = "11440")
        private String regionCode;

        @Schema(description = "시·군·구 이름 (시·도 이름 제외, 세종은 세종특별자치시)", example = "마포구")
        private String sigunguName;
    }
}
