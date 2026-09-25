package com.hyeja.domain.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeja.domain.policy.dto.PolicyApiResponseDTO.PolicyItem;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseTextConfig;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiPolicyCategoryClassifier implements PolicyCategoryClassifier {
    private static final String SYSTEM_PROMPT = """
            당신은 대한민국 청년 주거정책을 하나의 하위 유형으로 분류하는 분석기다.

            category는 반드시 다음 값 중 하나여야 한다.
            - MONTHLY_RENT: 월세, 월 임차료 또는 월세 보증금 지원
            - JEONSE: 전세자금, 전세보증금, 전세대출 또는 전세보증 지원
            - PURCHASE: 주택 구입, 청약, 분양 또는 구입자금 지원
            - PUBLIC_RENT: 공공임대주택, 행복주택, 청년주택, 매입임대 또는 전세임대 입주
            - OTHER: 기숙사, 이사비, 중개비, 상담, 주거환경 개선 또는 명확히 분류되지 않음

            여러 유형이 포함되면 정책의 핵심 지원 목적을 기준으로 하나만 선택한다.
            근거가 부족하면 OTHER를 선택하고, 정책에 없는 내용은 추측하지 않는다.
            confidence는 0.0에서 1.0 사이의 숫자로 반환한다.
            reason은 판단 근거를 한국어 한 문장으로 반환한다.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "category", Map.of(
                            "type", "string",
                            "enum", List.of("MONTHLY_RENT", "JEONSE", "PURCHASE", "PUBLIC_RENT", "OTHER")),
                    "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                    "reason", Map.of("type", "string")),
            "required", List.of("category", "confidence", "reason"),
            "additionalProperties", false);

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Override
    public PolicyCategoryClassification classify(PolicyItem item) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(model)
                .inputOfResponse(List.of(
                        message(EasyInputMessage.Role.SYSTEM, SYSTEM_PROMPT),
                        message(EasyInputMessage.Role.USER, buildUserPrompt(item))))
                .text(ResponseTextConfig.builder()
                        .format(ResponseFormatTextJsonSchemaConfig.builder()
                                .name("policy_category_classification")
                                .strict(true)
                                .schema(JsonValue.from(RESPONSE_SCHEMA)
                                        .convert(ResponseFormatTextJsonSchemaConfig.Schema.class))
                                .build())
                        .build())
                .build();

        Response response = openAIClient.responses().create(params);
        String responseText = response.output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OpenAI 카테고리 분류 응답이 비어 있습니다."));

        AiCategoryResponse result = parseResponse(responseText);
        PolicyCategoryClassification classification = new PolicyCategoryClassification(
                PolicyCategory.valueOf(result.category()), result.confidence(), result.reason());
        log.info("AI 정책 카테고리 분류 - policyId={}, category={}, confidence={}, reason={}",
                item.getPolicyId(), classification.category(),
                classification.confidence(), classification.reason());
        return classification;
    }

    private ResponseInputItem message(EasyInputMessage.Role role, String content) {
        return ResponseInputItem.ofEasyInputMessage(EasyInputMessage.builder()
                .role(role)
                .content(content)
                .build());
    }

    String buildUserPrompt(PolicyItem item) {
        return """
                다음 정책을 주거정책 하위 유형으로 분류하라.

                정책명: %s
                API 중분류: %s
                주거 유형: %s
                키워드: %s
                정책 설명: %s
                지원 내용: %s
                추가 자격조건: %s
                """.formatted(
                valueOrEmpty(item.getPolicyName()),
                valueOrEmpty(item.getSubCategory()),
                valueOrEmpty(item.getSubCategory()),
                valueOrEmpty(item.getKeywords()),
                valueOrEmpty(item.getPolicyExplanation()),
                valueOrEmpty(item.getSupportContent()),
                valueOrEmpty(item.getExtraQualification()));
    }

    private AiCategoryResponse parseResponse(String responseText) {
        try {
            return objectMapper.readValue(responseText, AiCategoryResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OpenAI 카테고리 분류 응답을 해석할 수 없습니다.", exception);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record AiCategoryResponse(String category, double confidence, String reason) {
    }
}
