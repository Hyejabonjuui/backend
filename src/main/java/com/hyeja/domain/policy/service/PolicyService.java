package com.hyeja.domain.policy.service;

import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final RestTemplate restTemplate;

    @Value("${youth.api.key}")
    private String apiKey;

    @Value("${youth.api.url}")
    private String apiUrl;

    /**
     * 온통청년 Open API를 호출하여 주거 정책 데이터를 가져와 DB에 저장/동기화합니다.
     */
    @Transactional
    public void fetchAndSaveHousingPolicies() {
        // TODO: 실제 Open API 요청 파라미터 조합 및 호출 로직 구현
        // 예시 URL 조합: String requestUrl = apiUrl + "?openApiVlak=" + apiKey + "&display=100&pageIndex=1";
        
        // Open API 응답을 받아와 Policy 엔티티로 변환 후 policyRepository.saveAll()을 수행하는 로직이 들어갑니다.
    }

    /**
     * DB에 저장된 주거 정책 목록을 마감일 임박 순으로 조회합니다.
     */
    public List<Policy> getHousingPolicies() {
        return policyRepository.findByCategoryOrderByApplyEndDateAsc("주거");
    }
}