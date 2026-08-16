package com.chuckchuck.voice;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** OpenAI Structured Outputs로 허용된 Intent 중 하나만 반환받는다. */
@Component
public class OpenAiIntentClassifier implements IntentClassifier {
    private static final String SYSTEM_PROMPT = """
            당신은 어르신 음성 비서 '척척'의 의도 분류기입니다.
            사용자의 문장을 다음 중 정확히 하나로 분류하세요.
            YOUTUBE_PLAY: 유튜브 영상 검색 또는 실행
            WEATHER_INFO: 현재 또는 미래의 날씨, 기온, 비, 눈, 우산 관련 요청
            MAP_ROUTE: 장소까지 지도나 길찾기 요청
            KIOSK_HELP: 키오스크 사용 연습 요청
            UNKNOWN: 나머지 요청
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiIntentClassifier(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
            @Value("${OPENAI_INTENT_MODEL:gpt-4o-mini}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Intent classify(String userText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorCode.EXTERNAL_API_FAIL,
                    "OPENAI_API_KEY가 설정되지 않았습니다.",
                    "의도 분석 설정을 확인해 주세요."
            );
        }

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(userText))
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return parseIntent(response);
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(
                    ErrorCode.EXTERNAL_API_FAIL,
                    "OpenAI 의도 분석 요청에 실패했습니다.",
                    "지금은 말씀을 이해하기 어려워요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private Map<String, Object> requestBody(String userText) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "intent", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "YOUTUBE_PLAY", "WEATHER_INFO", "MAP_ROUTE", "KIOSK_HELP", "UNKNOWN"
                                )
                        )
                ),
                "required", List.of("intent"),
                "additionalProperties", false
        );
        Map<String, Object> responseFormat = Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "intent_classification",
                        "strict", true,
                        "schema", schema
                )
        );
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userText)
                ),
                "response_format", responseFormat,
                "temperature", 0
        );
    }

    private Intent parseIntent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw invalidResponse();
        }
        Choice first = response.choices().getFirst();
        if (first == null || first.message() == null) {
            throw invalidResponse();
        }
        String content = first.message().content();
        if (content == null || content.isBlank()) {
            throw invalidResponse();
        }
        try {
            Classification classification = objectMapper.readValue(content, Classification.class);
            if (classification.intent() == null) {
                throw invalidResponse();
            }
            return Intent.valueOf(classification.intent());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw invalidResponse();
        }
    }

    private ApiException invalidResponse() {
        return new ApiException(
                ErrorCode.EXTERNAL_API_FAIL,
                "OpenAI가 올바른 의도 분석 결과를 반환하지 않았습니다.",
                "지금은 말씀을 이해하기 어려워요. 다시 한번 말씀해 주세요."
        );
    }

    private record Classification(String intent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {
    }
}
