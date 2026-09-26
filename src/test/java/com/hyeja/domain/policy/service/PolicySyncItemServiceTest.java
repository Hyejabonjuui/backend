package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.cardnews.entity.CardNews;
import com.hyeja.domain.cardnews.repository.CardNewsRepository;
import com.hyeja.domain.policy.converter.PolicyApiCodeConverter;
import com.hyeja.domain.policy.converter.PolicyApiConverter;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.enums.PolicyIncomeCondition;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.policy.repository.PolicyRegionRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class PolicySyncItemServiceTest {
    private final PolicyRepository policyRepository = mock(PolicyRepository.class);
    private final CardNewsRepository cardNewsRepository = mock(CardNewsRepository.class);
    private final PolicyApiConverter policyApiConverter =
            new PolicyApiConverter(new PolicyApiCodeConverter());
    private final PolicyRegionRepository policyRegionRepository = mock(PolicyRegionRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final PolicySyncItemService service = new PolicySyncItemService(
            policyRepository, cardNewsRepository, policyApiConverter,
            policyRegionRepository, regionRepository);

    @Test
    void savesPolicyAndInitialCardNewsTogether() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("policy-1");
        item.setPolicyName("청년 주거 정책");
        item.setSupportContent("지원 내용");
        item.setRegionCodes("11110, 11440");
        PolicyAiAnalysis analysis = new PolicyAiAnalysis(
                PolicyCategory.OTHER, 0.8, "기타",
                null, 0.5, "확인 필요",
                PolicyIncomeCondition.UNKNOWN, null, null,
                0.5, "확인 필요");
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardNewsRepository.existsByPolicy_PolicyIdAndCardNo("policy-1", 1L))
                .thenReturn(false);
        Region firstRegion = Region.builder()
                .regionCode("11110").sigunguName("서울특별시 종로구").build();
        Region secondRegion = Region.builder()
                .regionCode("11440").sigunguName("서울특별시 마포구").build();
        when(regionRepository.findAllById(any())).thenReturn(List.of(firstRegion, secondRegion));
        AtomicReference<CardNews> savedCardNews = new AtomicReference<>();
        when(cardNewsRepository.save(any(CardNews.class))).thenAnswer(invocation -> {
            CardNews cardNews = invocation.getArgument(0);
            savedCardNews.set(cardNews);
            return cardNews;
        });

        service.save(item, analysis);

        verify(policyRepository).save(any(Policy.class));
        verify(policyRegionRepository).deleteAllByPolicy_PolicyId("policy-1");
        verify(policyRegionRepository).saveAll(org.mockito.ArgumentMatchers.argThat(regions -> {
            java.util.List<com.hyeja.domain.policy.entity.PolicyRegion> values = new java.util.ArrayList<>();
            regions.forEach(values::add);
            return values.size() == 2
                    && values.stream().map(value -> value.getRegion().getRegionCode())
                    .collect(java.util.stream.Collectors.toSet())
                    .equals(java.util.Set.of("11110", "11440"));
        }));
        assertThat(savedCardNews.get()).isNotNull();
        assertThat(savedCardNews.get().getPolicy().getPolicyId()).isEqualTo("policy-1");
        assertThat(savedCardNews.get().getCardNo()).isEqualTo(1L);
        assertThat(savedCardNews.get().getBody()).isEqualTo("지원 내용");
    }

    @Test
    void saveUsesIndependentTransaction() throws NoSuchMethodException {
        Transactional transactional = PolicySyncItemService.class
                .getMethod("save", PolicyItem.class, PolicyAiAnalysis.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void expandsSidoCodeToAllSigunguRegions() {
        PolicyItem item = new PolicyItem();
        item.setPolicyId("seoul-policy");
        item.setPolicyName("서울특별시 지원 정책");
        item.setRegionCodes("11000");
        PolicyAiAnalysis analysis = new PolicyAiAnalysis(
                PolicyCategory.OTHER, 0.8, "기타",
                null, 0.5, "확인 필요",
                PolicyIncomeCondition.UNKNOWN, null, null,
                0.5, "확인 필요");
        Region jongno = Region.builder()
                .regionCode("11110").sigunguName("서울특별시 종로구").build();
        Region gangnam = Region.builder()
                .regionCode("11680").sigunguName("서울특별시 강남구").build();
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(regionRepository.findAllByRegionCodeStartingWith("11"))
                .thenReturn(List.of(jongno, gangnam));
        when(cardNewsRepository.existsByPolicy_PolicyIdAndCardNo("seoul-policy", 1L))
                .thenReturn(true);

        service.save(item, analysis);

        verify(regionRepository).findAllByRegionCodeStartingWith("11");
        verify(policyRegionRepository).saveAll(org.mockito.ArgumentMatchers.argThat(regions -> {
            java.util.List<com.hyeja.domain.policy.entity.PolicyRegion> values =
                    new java.util.ArrayList<>();
            regions.forEach(values::add);
            return values.stream().map(value -> value.getRegion().getRegionCode())
                    .collect(java.util.stream.Collectors.toSet())
                    .equals(java.util.Set.of("11110", "11680"));
        }));
    }
}
