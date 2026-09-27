package com.hyeja.domain.term.converter;

import com.hyeja.domain.term.dto.TermResponseDTO;
import com.hyeja.domain.term.entity.Term;

public final class TermConverter {

    private TermConverter() {
    }

    public static TermResponseDTO toResponseDTO(Term term) {
        return TermResponseDTO.builder()
                .termId(term.getTermId())
                .term(term.getTerm())
                .easyDescription(term.getEasyDescription())
                .example(term.getExample())
                .build();
    }
}
