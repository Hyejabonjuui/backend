package com.hyeja.domain.cardnews.dto;

import java.time.LocalDate;

public record MemberCardNewsResponseDTO(
        String policyId,
        String policyName,
        String description,
        LocalDate applyEndDate
) {
}
