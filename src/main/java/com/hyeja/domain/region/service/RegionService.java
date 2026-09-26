package com.hyeja.domain.region.service;

import com.hyeja.domain.region.converter.RegionConverter;
import com.hyeja.domain.region.dto.RegionResponseDTO.SidoDTO;
import com.hyeja.domain.region.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    /**
     * 전체 지역(시군구 269건)을 코드 순으로 읽어 시·도별로 묶어 돌려줍니다.
     * 건수가 작아 필터 없이 한 번에 내려주고, 프론트가 드롭다운에서 시·도를 골라 거릅니다.
     */
    public List<SidoDTO> getRegions() {
        return RegionConverter.toSidoDTOs(regionRepository.findAll(Sort.by("regionCode")));
    }
}
