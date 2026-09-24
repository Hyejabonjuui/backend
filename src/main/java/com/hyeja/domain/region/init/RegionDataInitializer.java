package com.hyeja.domain.region.init;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.hyeja.domain.region.dto.RegionCsvDto;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionDataInitializer implements CommandLineRunner {

    private final RegionRepository regionRepository;
    private final Environment environment; // 환경 정보를 읽기 위한 주입
    
    @Override
    public void run(String... args) throws Exception {
        // [중요] 테스트 환경("test")인 경우 CSV 로드 및 초기화 로직을 실행하지 않음
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("test")) {
            log.info("[RegionDataInitializer] Test environment detected. Skipping CSV initialization.");
            return;
        }
        // 1. 이미 데이터가 존재한다면 실행하지 않음 (최초 1회 보장) - seed데이터로 인해 임시로 10이 아닐 때 reload하도록 설정
        if (regionRepository.count() >0) {
            log.info("[RegionDataInitializer] Region data already exists. Skipping initialization.");
            return;
        }

        log.info("[RegionDataInitializer] Starting region CSV preprocessing and upload...");

        ClassPathResource resource = new ClassPathResource("csv/국토교통부_법정동코드_20260813.csv");
        
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("EUC-KR")))) {
            
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema csvSchema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnReordering(true);

            MappingIterator<RegionCsvDto> mappingIterator = csvMapper
                    .readerFor(RegionCsvDto.class)
                    .with(csvSchema)
                    .readValues(reader);

            List<RegionCsvDto> csvDataList = mappingIterator.readAll();

            // 2. 전처리 및 중복 제거 로직 (Collectors.toMap을 이용해 regionCode 기준 중복 제거)
            Map<String, Region> uniqueRegionsMap = csvDataList.stream()
                    .filter(dto -> dto.getRegionCode() != null && dto.getRegionCode().trim().length() >= 5)
                    .map(dto -> {
                        // 규칙 1: regionCode 앞 5자리 추출
                        String rawCode = dto.getRegionCode().trim();
                        String regionCode = rawCode.substring(0, 5);

                        // 규칙 2: sigunguName 공백 기준 최대 2단어 추출 (예: "서울특별시 종로구 청운동" -> "서울특별시 종로구")
                        String rawName = dto.getSigunguName() != null ? dto.getSigunguName().trim() : "";
                        String sigunguName = extractSigunguName(rawName);

                        return Region.builder()
                                .regionCode(regionCode)
                                .sigunguName(sigunguName)
                                .build();
                    })
                    // 규칙 3: regionCode가 중복될 경우 첫 번째 데이터를 유지하고 나머지 중복은 버림
                    .collect(Collectors.toMap(
                            Region::getRegionCode,
                            region -> region,
                            (existing, replacement) -> existing
                    ));

            List<Region> regions = List.copyOf(uniqueRegionsMap.values());

            // 3. DB에 일괄 저장
            regionRepository.saveAll(regions);
            log.info("[RegionDataInitializer] Successfully preprocessed and uploaded {} unique regions to MariaDB!", regions.size());
            
        } catch (Exception e) {
            log.error("[RegionDataInitializer] Failed to upload region CSV data", e);
        }
    }

    /**
     * 시군구 이름을 공백 기준으로 최대 2단어까지만 추출하는 헬퍼 메서드
     * 예: "서울특별시" -> "서울특별시"
     * 예: "서울특별시 종로구 청운동" -> "서울특별시 종로구"
     */
    private String extractSigunguName(String fullName) {
        if (fullName.isEmpty()) {
            return "";
        }
        String[] words = fullName.split("\\s+"); // 공백 기준 분리
        if (words.length >= 2) {
            return words[0] + " " + words[1];
        }
        return words[0];
    }
}