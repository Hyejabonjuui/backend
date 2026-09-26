package com.hyeja.domain.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Test;

class OpenAiPolicyAnalyzerTest {

    private final OpenAiPolicyAnalyzer analyzer = new OpenAiPolicyAnalyzer(
            mock(OpenAIClient.class), new ObjectMapper());

    @Test
    void mapsHouselessRequirementToNullableBoolean() {
        assertThat(analyzer.toHouselessYn("REQUIRED")).isTrue();
        assertThat(analyzer.toHouselessYn("NOT_REQUIRED")).isFalse();
        assertThat(analyzer.toHouselessYn("UNKNOWN")).isNull();
    }

    @Test
    void includesQualificationFieldsInPrompt() {
        PolicyItem item = new PolicyItem();
        item.setExtraQualification("무주택 세대구성원");
        item.setParticipantTarget("청년 무주택자");
        item.setApplyMethod("온라인 신청");
        item.setIncomeConditionCode("0043002");
        item.setIncomeMax("50000000");
        item.setIncomeEtc("개인 연소득 5천만원 이하");

        assertThat(analyzer.buildUserPrompt(item))
                .contains("무주택 세대구성원", "청년 무주택자", "온라인 신청",
                        "0043002", "50000000", "개인 연소득 5천만원 이하");
    }
}
