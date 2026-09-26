package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.favorite.repository.FavoriteRepository;
import com.hyeja.domain.policy.converter.PolicyConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO;
import com.hyeja.domain.policy.dto.PolicyResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.entity.PolicyRegion;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicySort;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.profile.entity.Profile;
import com.hyeja.domain.profile.service.ProfileService;
import com.hyeja.domain.region.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CardNewsRepository cardNewsRepository;
    private final RestTemplate restTemplate;
    private final ProfileService profileService;
    private final PolicyRegionRepository policyRegionRepository;
    private final FavoriteRepository favoriteRepository;

    @Value("${youth.api.key}")
    private String apiKey;

    @Value("${youth.api.url}")
    private String apiUrl;

    /**
     * 온통청년 Open API를 호출하여 실제 데이터를 받아오고, 
     * 그중 "주거" 관련 정책을 필터링하여 DB에 적재합니다.
     */
    @Transactional
    public void fetchAndSaveHousingPolicies() {
        try {
            URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("apiKeyNm", apiKey)
                    .queryParam("pageNum", 1)
                    .queryParam("pageSize", 100) // 필요한 만큼즈 조절 가능
                    .queryParam("rtnType", "json")
                    .encode()
                    .build()
                    .toUri();

            // API 키가 쿼리 파라미터에 포함되므로 전체 URI를 로그에 남기지 않습니다.
            log.info("온통청년 정책 API 요청을 시작합니다.");

            // 1. API 호출 후 DTO로 바로 매핑
            PolicyApiResponseDTO response = restTemplate.getForObject(uri, PolicyApiResponseDTO.class);

            if (response == null || response.getResult() == null || response.getResult().getYouthPolicyList() == null) {
                log.warn("API로부터 가져온 정책 데이터가 비어있습니다.");
                return;
            }

            int savedCount = 0;

            // 2. 정책 리스트 순회
            for (PolicyApiResponseDTO.PolicyItem item : response.getResult().getYouthPolicyList()) {
                // "주거" 관련 정책만 필터링 (필요에 따라 조건 수정 가능)
                if ("주거".equals(item.getCategory())) {
                    LocalDate endDate = parseApplyEndDate(item.getApplyYmd());

                    // Policy 엔티티 생성
                    Policy policy = Policy.builder()
                            .policyId(item.getPolicyId())
                            .policyName(item.getPolicyName() != null ? item.getPolicyName() : "제목 없음")
                            // 외부 API의 "주거"는 대분류이므로 세부 분류 정보가 없으면 기타로 저장합니다.
                            .category(PolicyCategory.OTHER)
                            .description(item.getDescription())
                            .supportContent(item.getSupportContent())
                            .applyPeriodCode("003") // 기본 코드 또는 파싱 값
                            .applyEndDate(endDate)
                            .applyUrl(item.getApplyUrl())
                            .ageLimitYn(false) // 필수 필드 기본값
                            .build();

                    // DB 저장 (Upsert)
                    policyRepository.save(policy);

                    // 3. 홈 화면 카드뉴스(card_no = 1) 자동 매핑 (중복 체크)
                    boolean cardNewsExists = cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), 1L);
                    if (!cardNewsExists) {
                        String bodyText = policy.getDescription();
                        if (bodyText == null || bodyText.isBlank()) {
                            bodyText = "주거 지원 정책 요약 정보가 없습니다.";
                        } else if (bodyText.length() > 50) {
                            bodyText = bodyText.substring(0, 50) + "...";
                        }

                        CardNews cardNews = CardNews.builder()
                                .policy(policy)
                                .title(policy.getPolicyName())
                                .body(bodyText)
                                .cardNo(1L) // 홈화면 조회용 대표 카드 번호
                                .build();
                        cardNewsRepository.save(cardNews);
                    }
                    savedCount++;
                }
            }

            log.info("실제 온통청년 '주거' 정책 데이터 총 {}건 적재 완료", savedCount);

        } catch (Exception e) {
            log.error("정책 데이터 동기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("정책 데이터 동기화 실패", e);
        }
    }

    /**
     * 신청 기간 문자열(예: "20260923 ~ 20260928")에서 종료일(마감일)을 파싱합니다.
     */
    private LocalDate parseApplyEndDate(String applyYmd) {
        if (applyYmd == null || applyYmd.isBlank()) return null;
        try {
            // "~"를 포함한 기간 형태인 경우 뒷부분(종료일)을 추출
            if (applyYmd.contains("~")) {
                String endDateStr = applyYmd.split("~")[1].trim();
                // 날짜 포맷이 8자리 숫자(예: 20260928)인 경우
                if (endDateStr.length() >= 8) {
                    String cleanDate = endDateStr.substring(0, 8);
                    return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                }
            }
        } catch (Exception e) {
            log.warn("마감일 파싱 실패 - 원본 문자열: {}", applyYmd);
        }
        return null;
    }

    public List<Policy> getHousingPolicies() {
        return policyRepository.findAllByOrderByApplyEndDateAsc();
    }

    public PolicyListDTO getHousingPoliciesForMember(
            Long memberId,
            PolicyCategory category,
            PolicySort sort,
            boolean onlyEligible,
            int page,
            int size
    ) {
        Profile profile = profileService.getActiveProfile(memberId);
        LocalDate today = LocalDate.now();
        Page<Policy> policyPage = policyRepository.findHousingPoliciesForMember(
                category,
                onlyEligible,
                today,
                Period.between(profile.getBirth(), today).getYears(),
                profile.getHouselessYn(),
                profile.getEmploymentCode().name(),
                profile.getRegion().getRegionCode(),
                PageRequest.of(page, size, sort.toSort())
        );

        List<String> policyIds = policyPage.getContent().stream()
                .map(Policy::getPolicyId)
                .toList();
        Map<String, List<Region>> regionsByPolicyId = policyIds.isEmpty()
                ? Collections.emptyMap()
                : policyRegionRepository.findAllActiveByPolicyIds(policyIds).stream()
                        .collect(Collectors.groupingBy(
                                policyRegion -> policyRegion.getPolicy().getPolicyId(),
                                Collectors.mapping(PolicyRegion::getRegion, Collectors.toList())
                        ));
        Set<String> favoritePolicyIds = policyIds.isEmpty()
                ? Collections.emptySet()
                : favoriteRepository.findActivePolicyIds(memberId, policyIds);

        return PolicyConverter.toPolicyListDTO(
                policyPage, regionsByPolicyId, favoritePolicyIds, today);
    }
}
