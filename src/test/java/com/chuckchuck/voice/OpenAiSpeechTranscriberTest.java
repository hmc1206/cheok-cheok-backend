package com.chuckchuck.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

class OpenAiSpeechTranscriberTest {

    @Test
    void sendsMultipartAudioAndReturnsTrimmedText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiSpeechTranscriber transcriber = new OpenAiSpeechTranscriber(
                builder, "test-key", "https://api.openai.com/v1", "gpt-4o-mini-transcribe"
        );
        String encoded = Base64.getEncoder()
                .encodeToString("audio-data".getBytes(StandardCharsets.UTF_8));

        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("gpt-4o-mini-transcribe")))
                .andExpect(content().string(containsString("speech.webm")))
                .andRespond(withSuccess("{\"text\":\" 미스트롯 영상 틀어줘 \"}", MediaType.APPLICATION_JSON));

        assertThat(transcriber.transcribe("data:audio/webm;base64," + encoded))
                .isEqualTo("미스트롯 영상 틀어줘");
        server.verify();
    }

    @Test
    void rejectsMalformedBase64() {
        OpenAiSpeechTranscriber transcriber = new OpenAiSpeechTranscriber(
                RestClient.builder(), "test-key", "https://api.openai.com/v1", "gpt-4o-mini-transcribe"
        );

        assertThatThrownBy(() -> transcriber.transcribe("not-base64"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingApi() {
        OpenAiSpeechTranscriber transcriber = new OpenAiSpeechTranscriber(
                RestClient.builder(), "", "https://api.openai.com/v1", "gpt-4o-mini-transcribe"
        );

        assertThatThrownBy(() -> transcriber.transcribe("YXVkaW8="))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAIL));
    }
}
