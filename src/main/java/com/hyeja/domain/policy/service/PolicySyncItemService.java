package com.hyeja.domain.policy.service;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicySyncItemService {
    private static final int CARD_BODY_MAX_LENGTH = 500;

    private final PolicyRepository policyRepository;
    private final CardNewsRepository cardNewsRepository;
    private final PolicyApiConverter policyApiConverter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PolicyItem item, PolicyAiAnalysis analysis) {
        Policy policy = policyRepository.save(policyApiConverter.convert(
                item, analysis.category(), analysis.houselessYn(),
                analysis.incomeCondition(), analysis.incomeMin(), analysis.incomeMax()));
        createTestCardNewsIfAbsent(policy);
    }

    private void createTestCardNewsIfAbsent(Policy policy) {
        if (cardNewsRepository.existsByPolicy_PolicyIdAndCardNo(policy.getPolicyId(), 1L)) {
            return;
        }

        String body = defaultIfBlank(policy.getSupportContent(), "지원 내용이 등록되지 않았습니다.");
        if (body.length() > CARD_BODY_MAX_LENGTH) {
            body = body.substring(0, CARD_BODY_MAX_LENGTH - 3) + "...";
        }

        cardNewsRepository.save(CardNews.builder()
                .policy(policy)
                .title(policy.getPolicyName())
                .body(body)
                .cardNo(1L)
                .build());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
