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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
        log.info("[RegionDataInitializer] Starting region CSV preprocessing and upload...");

        // 시군구 269건. 출처·가공 방법·갱신 방법은 CSV 파일 맨 위 # 주석에 적어 두었습니다.
        ClassPathResource resource = new ClassPathResource("csv/region_sigungu.csv");

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema csvSchema = CsvSchema.emptySchema()
                    .withHeader()
                    .withComments() // # 으로 시작하는 줄(출처 설명)은 건너뜁니다.
                    .withColumnReordering(true);

            MappingIterator<RegionCsvDto> mappingIterator = csvMapper
                    .readerFor(RegionCsvDto.class)
                    .with(csvSchema)
                    .readValues(reader);

            List<RegionCsvDto> csvDataList = mappingIterator.readAll();

            // 1. 이미 DB에 있는 코드는 건너뜁니다. 시드(dev-data.sql)가 일부 지역을 먼저 넣어도 나머지를 채우고,
            //    서버를 다시 켜도 중복 저장되지 않습니다. (예전에는 count() > 0이면 건너뛰어, 새 DB에서 시드 10건만 남았습니다.)
            Set<String> existingCodes = regionRepository.findAll().stream()
                    .map(Region::getRegionCode)
                    .collect(Collectors.toSet());

            // 2. CSV 값을 그대로 저장합니다. 예: 41111 / "경기도 수원시 장안구"
            List<Region> newRegions = csvDataList.stream()
                    .map(dto -> Region.builder()
                            .regionCode(dto.getRegionCode().trim())
                            .sigunguName(dto.getSigunguName().trim())
                            .build())
                    .filter(region -> !existingCodes.contains(region.getRegionCode()))
                    .toList();

            if (newRegions.isEmpty()) {
                log.info("[RegionDataInitializer] Region data already loaded. Nothing to add.");
                return;
            }

            // 3. DB에 일괄 저장
            regionRepository.saveAll(newRegions);
            log.info("[RegionDataInitializer] Uploaded {} new regions.", newRegions.size());

        } catch (Exception e) {
            log.error("[RegionDataInitializer] Failed to upload region CSV data", e);
        }
    }
}