package com.hyeja.domain.term.service;

import com.hyeja.domain.term.converter.TermConverter;
import com.hyeja.domain.term.dto.TermResponseDTO;
import com.hyeja.domain.term.repository.TermRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermService {

    private final TermRepository termRepository;

    public List<TermResponseDTO> getTerms() {
        return termRepository.findAllByDeletedAtIsNullOrderByTermIdAsc().stream()
                .map(TermConverter::toResponseDTO)
                .toList();
    }
}
