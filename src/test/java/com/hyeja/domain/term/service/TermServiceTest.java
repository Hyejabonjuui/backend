package com.hyeja.domain.term.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.term.dto.TermResponseDTO;
import com.hyeja.domain.term.entity.Term;
import com.hyeja.domain.term.repository.TermRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {

    @Mock
    private TermRepository termRepository;

    @InjectMocks
    private TermService termService;

    @Test
    void returnsConvertedTermList() {
        Term term = Term.builder()
                .term("중위소득")
                .easyDescription("전체 가구를 소득 순서로 세웠을 때 가운데 가구의 소득")
                .example("중위소득 60% 이하")
                .build();
        ReflectionTestUtils.setField(term, "termId", 1);
        when(termRepository.findAllByDeletedAtIsNullOrderByTermIdAsc()).thenReturn(List.of(term));

        List<TermResponseDTO> result = termService.getTerms();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getTermId()).isEqualTo(1);
            assertThat(item.getTerm()).isEqualTo("중위소득");
            assertThat(item.getEasyDescription()).isEqualTo("전체 가구를 소득 순서로 세웠을 때 가운데 가구의 소득");
            assertThat(item.getExample()).isEqualTo("중위소득 60% 이하");
        });
        verify(termRepository).findAllByDeletedAtIsNullOrderByTermIdAsc();
    }
}
