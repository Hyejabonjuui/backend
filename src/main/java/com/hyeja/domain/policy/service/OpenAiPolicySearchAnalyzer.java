package com.hyeja.domain.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeja.domain.policy.dto.PolicyDetailResponseDTO.ConditionResultDTO;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.ReasonRequest;
import com.hyeja.domain.policy.service.PolicySearchAiAnalyzer.SearchIntent;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseTextConfig;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiPolicySearchAnalyzer implements PolicySearchAiAnalyzer {
    private static final String INTENT_PROMPT = """
            사용자의 질문이 청년 주거 정책 검색인지 판별하고 관련 카테고리를 고르세요.
            카테고리는 MONTHLY_RENT, JEONSE, PURCHASE, PUBLIC_RENT, OTHER 중 하나 이상입니다.
            월세는 MONTHLY_RENT, 전세는 JEONSE, 청약·주택 구입은 PURCHASE,
            공공임대는 PUBLIC_RENT이며 그 밖의 주거 지원은 OTHER입니다.
            취업·창업·교육처럼 주거와 무관한 질문은 housingRelated를 false로 반환하세요.
            명시되지 않은 의도를 추측하지 마세요.
            """;
    private static final String REASON_PROMPT = """
            청년 주거 정책 검색 결과에 표시할 개인화 이유를 작성하세요.
            evaluation과 overallStatus는 서버가 확정한 값이므로 절대 변경하지 마세요.
            제공된 회원 정보와 정책 조건만 사용하고 없는 사실은 추측하지 마세요.
            ABLE 결과는 충족한 핵심 근거를, UNKNOWN 결과는 추가 확인할 정보나 조건을,
            DISABLE 결과는 맞지 않는 조건과 회원 값을 우선하여 자연스러운 존댓말 한 문장으로 쓰세요.
            각 문장은 간결하게 작성하세요.
            """;
    private static final Map<String, Object> INTENT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "housingRelated", Map.of("type", "boolean"),
                    "categories", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string", "enum", List.of(
                                    "MONTHLY_RENT", "JEONSE", "PURCHASE", "PUBLIC_RENT", "OTHER")))),
            "required", List.of("housingRelated", "categories"),
            "additionalProperties", false);
    private static final Map<String, Object> REASON_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("reasons", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "policyId", Map.of("type", "string"),
                                    "reason", Map.of("type", "string", "minLength", 1)),
                            "required", List.of("policyId", "reason"),
                            "additionalProperties", false))),
            "required", List.of("reasons"),
            "additionalProperties", false);

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Override
    public SearchIntent analyzeIntent(String query) {
        IntentResponse response = parse(call(INTENT_PROMPT, query, "policy_search_intent", INTENT_SCHEMA),
                IntentResponse.class);
        Set<PolicyCategory> categories = response.categories() == null
                ? Set.of()
                : response.categories().stream()
                        .map(PolicyCategory::valueOf)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new SearchIntent(response.housingRelated(), Set.copyOf(categories));
    }

    @Override
    public Map<String, String> generateReasons(List<ReasonRequest> requests) {
        String input = requests.stream().map(this::reasonInput)
                .collect(java.util.stream.Collectors.joining("\n\n"));
        ReasonsResponse response = parse(call(REASON_PROMPT, input, "policy_search_reasons", REASON_SCHEMA),
                ReasonsResponse.class);
        Map<String, String> reasons = new LinkedHashMap<>();
        if (response.reasons() != null) {
            response.reasons().forEach(item -> {
                if (item.policyId() != null && item.reason() != null && !item.reason().isBlank()) {
                    reasons.putIfAbsent(item.policyId(), item.reason().trim());
                }
            });
        }
        return reasons;
    }

    private String reasonInput(ReasonRequest request) {
        String conditions = request.conditions().stream()
                .map(this::conditionLine)
                .collect(java.util.stream.Collectors.joining("\n"));
        return """
                policyId: %s
                policyName: %s
                overallStatus: %s
                housingType: %s
                memberRegion: %s
                evaluation:
                %s
                """.formatted(
                request.policy().getPolicyId(), request.policy().getPolicyName(),
                request.overallStatus(),
                request.profile().getHousingType() == null
                        ? "미입력" : request.profile().getHousingType().getLabel(),
                request.profile().getRegion() == null
                        ? "미입력" : request.profile().getRegion().getSigunguName(),
                conditions);
    }

    private String conditionLine(ConditionResultDTO condition) {
        return "- %s: status=%s, policyCondition=%s, memberValue=%s".formatted(
                condition.type(), condition.status(), condition.policyCondition(), condition.memberValue());
    }

    private String call(String systemPrompt, String userPrompt, String schemaName,
            Map<String, Object> schema) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(model)
                .inputOfResponse(List.of(
                        message(EasyInputMessage.Role.SYSTEM, systemPrompt),
                        message(EasyInputMessage.Role.USER, userPrompt)))
                .text(ResponseTextConfig.builder()
                        .format(ResponseFormatTextJsonSchemaConfig.builder()
                                .name(schemaName)
                                .strict(true)
                                .schema(JsonValue.from(schema)
                                        .convert(ResponseFormatTextJsonSchemaConfig.Schema.class))
                                .build())
                        .build())
                .build();
        Response response = openAIClient.responses().create(params);
        return response.output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(output -> output.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OpenAI 검색 분석 응답이 비어 있습니다."));
    }

    private ResponseInputItem message(EasyInputMessage.Role role, String content) {
        return ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                .role(role).content(content).build());
    }

    private <T> T parse(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OpenAI 검색 분석 응답을 해석할 수 없습니다.", exception);
        }
    }

    private record IntentResponse(boolean housingRelated, List<String> categories) {
    }

    private record ReasonsResponse(List<ReasonItem> reasons) {
    }

    private record ReasonItem(String policyId, String reason) {
    }
}
