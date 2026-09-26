package com.hyeja.domain.policy.converter;

import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyListDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyListItemDTO;
import com.hyeja.domain.policy.dto.PolicyGuestResponseDTO.PolicyRegionItemDTO;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.region.entity.Region;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public final class PolicyGuestConverter {

    private PolicyGuestConverter() {
    }

    public static PolicyListDTO toPolicyListDTO(
            Page<Policy> policyPage,
            Map<String, List<Region>> regionsByPolicyId,
            LocalDate today
    ) {
        return PolicyListDTO.builder()
                .policies(policyPage.getContent().stream()
                        .map(policy -> toPolicyListItemDTO(
                                policy,
                                regionsByPolicyId.getOrDefault(policy.getPolicyId(), List.of()),
                                today
                        ))
                        .toList())
                .page(policyPage.getNumber())
                .size(policyPage.getSize())
                .totalElements(policyPage.getTotalElements())
                .totalPages(policyPage.getTotalPages())
                .hasNext(policyPage.hasNext())
                .build();
    }

    private static PolicyListItemDTO toPolicyListItemDTO(
            Policy policy,
            List<Region> regions,
            LocalDate today
    ) {
        return PolicyListItemDTO.builder()
                .policyId(policy.getPolicyId())
                .policyName(policy.getPolicyName())
                .categoryCode(policy.getCategory())
                .categoryName(policy.getCategory().getLabel())
                .regions(regions.stream()
                        .map(region -> PolicyRegionItemDTO.builder()
                                .regionCode(region.getRegionCode())
                                .regionName(region.getSigunguName())
                                .build())
                        .toList())
                .nationwide(regions.isEmpty())
                .applyEndDate(policy.getApplyEndDate())
                .applyPeriodCode(policy.getApplyPeriodCode())
                .dDay(policy.getApplyEndDate() == null
                        ? null
                        : Math.toIntExact(ChronoUnit.DAYS.between(today, policy.getApplyEndDate())))
                .build();
    }
}
