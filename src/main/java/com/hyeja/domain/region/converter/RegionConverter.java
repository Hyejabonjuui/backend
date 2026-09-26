package com.hyeja.domain.region.converter;

import com.hyeja.domain.region.dto.RegionResponseDTO.SidoDTO;
import com.hyeja.domain.region.dto.RegionResponseDTO.SigunguDTO;
import com.hyeja.domain.region.entity.Region;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 지역 엔티티 → 응답 DTO 변환을 모아 둡니다.
public class RegionConverter {

    /**
     * 코드 순으로 정렬된 지역 목록을 시·도별로 묶습니다.
     * REGION에는 시·도 컬럼이 없으므로 코드 앞 2자리를 시·도 코드로, 이름의 첫 단어를 시·도 이름으로 씁니다.
     * 예: 11440 "서울특별시 마포구" → 시·도 11 "서울특별시" / 시·군·구 11440 "마포구"
     */
    public static List<SidoDTO> toSidoDTOs(List<Region> regions) {
        // LinkedHashMap으로 묶어 코드 순서를 그대로 유지합니다.
        Map<String, List<Region>> bySido = regions.stream()
                .collect(Collectors.groupingBy(region -> region.getRegionCode().substring(0, 2),
                        LinkedHashMap::new, Collectors.toList()));

        return bySido.entrySet().stream()
                .map(entry -> {
                    String sidoName = entry.getValue().get(0).getSigunguName().split(" ")[0];
                    return SidoDTO.builder()
                            .sidoCode(entry.getKey())
                            .sidoName(sidoName)
                            .sigungu(entry.getValue().stream()
                                    .map(region -> toSigunguDTO(region, sidoName))
                                    .toList())
                            .build();
                })
                .toList();
    }

    // "서울특별시 마포구" → "마포구". 세종처럼 시·도와 이름이 같으면("세종특별자치시") 그대로 둡니다.
    private static SigunguDTO toSigunguDTO(Region region, String sidoName) {
        String name = region.getSigunguName();
        return SigunguDTO.builder()
                .regionCode(region.getRegionCode())
                .sigunguName(name.equals(sidoName) ? name : name.substring(sidoName.length() + 1))
                .build();
    }
}
