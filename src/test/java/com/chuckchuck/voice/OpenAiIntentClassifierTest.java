package com.chuckchuck.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OpenAiIntentClassifierTest {

    @Test
    void requestsStructuredOutputAndReturnsIntent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiIntentClassifier classifier = new OpenAiIntentClassifier(
                builder, new ObjectMapper(), "test-key", "https://api.openai.com/v1", "gpt-4o-mini"
        );

        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().string(containsString("json_schema")))
                .andExpect(content().string(containsString("MAP_ROUTE")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {"message": {"content": "{\\\"intent\\\":\\\"MAP_ROUTE\\\"}"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(classifier.classify("아들 집 가는 길 알려줘")).isEqualTo(Intent.MAP_ROUTE);
        server.verify();
    }

    @Test
    void rejectsMissingApiKey() {
        OpenAiIntentClassifier classifier = new OpenAiIntentClassifier(
                RestClient.builder(), new ObjectMapper(), "", "https://api.openai.com/v1", "gpt-4o-mini"
        );

        assertThatThrownBy(() -> classifier.classify("유튜브 틀어줘"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAIL));
    }
}
