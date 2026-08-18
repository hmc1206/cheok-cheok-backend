package com.chuckchuck.voice;

import java.util.Base64;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Base64 녹음을 파일 파트로 바꿔 OpenAI Transcription API에 전송한다.
 * API 키는 서버 환경 변수에서만 읽고 요청·응답 로그에는 남기지 않는다.
 */
@Component
public class OpenAiSpeechTranscriber implements SpeechTranscriber {
    private static final String DEFAULT_MIME_TYPE = "audio/webm";
    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiSpeechTranscriber(
            RestClient.Builder restClientBuilder,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl,
            @Value("${OPENAI_TRANSCRIPTION_MODEL:gpt-4o-mini-transcribe}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String transcribe(String audioBase64) {
        requireApiKey();
        AudioPayload payload = decode(audioBase64);

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        // ByteArrayResource는 기본 파일명이 없어서 API가 파일 파트로 인식하도록 이름을 직접 붙인다.
        body.part("file", audioResource(payload.bytes(), extensionFor(payload.mimeType())))
                .contentType(MediaType.parseMediaType(payload.mimeType()));
        body.part("model", model);
        body.part("language", "ko");

        try {
            TranscriptionResponse response = restClient.post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(TranscriptionResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new ApiException(ErrorCode.STT_EMPTY_INPUT);
            }
            return response.text().trim();
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ApiException(
                    ErrorCode.EXTERNAL_API_FAIL,
                    "OpenAI 음성 인식 요청에 실패했습니다.",
                    "지금은 말씀을 이해하기 어려워요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(
                    ErrorCode.EXTERNAL_API_FAIL,
                    "OPENAI_API_KEY가 설정되지 않았습니다.",
                    "지금은 이용할 수 없어요. 잠시 후 다시 해 주세요."
            );
        }
    }

    private AudioPayload decode(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String mimeType = DEFAULT_MIME_TYPE;
        String encoded = value;
        if (value.startsWith("data:")) {
            int separator = value.indexOf(',');
            int mimeEnd = value.indexOf(';');
            if (separator < 0 || mimeEnd < 5 || mimeEnd > separator) {
                throw invalidAudio();
            }
            mimeType = value.substring(5, mimeEnd).toLowerCase(Locale.ROOT);
            encoded = value.substring(separator + 1);
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length == 0 || bytes.length > MAX_AUDIO_BYTES) {
                throw new IllegalArgumentException("invalid audio size");
            }
            return new AudioPayload(bytes, supportedMimeType(mimeType));
        } catch (IllegalArgumentException exception) {
            throw invalidAudio();
        }
    }

    private String supportedMimeType(String mimeType) {
        return switch (mimeType) {
            case "audio/webm", "audio/mp4", "audio/x-m4a", "audio/mpeg", "audio/mp3",
                    "audio/ogg", "audio/wav", "audio/x-wav", "audio/flac" -> mimeType;
            default -> throw invalidAudio();
        };
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "audio/mp4", "audio/x-m4a" -> "m4a";
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/ogg" -> "ogg";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/flac" -> "flac";
            default -> "webm";
        };
    }

    private ByteArrayResource audioResource(byte[] audio, String extension) {
        return new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return "speech." + extension;
            }
        };
    }

    private ApiException invalidAudio() {
        return new ApiException(
                ErrorCode.INVALID_REQUEST,
                "audio Base64 형식 또는 파일 크기가 올바르지 않습니다.",
                "잘 못 들었어요. 다시 한번 말씀해 주세요."
        );
    }

    private record AudioPayload(byte[] bytes, String mimeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranscriptionResponse(String text) {
    }
}
