package com.chuckchuck.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.session.SessionService;
import com.chuckchuck.session.SessionState;

class VoiceServiceTest {
    private SessionService sessionService;
    private IntentClassifier classifier;
    private IntentRouter router;
    private IntentHandler handler;
    private SpeechTranscriber speechTranscriber;
    private VoiceService service;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        classifier = mock(IntentClassifier.class);
        router = mock(IntentRouter.class);
        handler = mock(IntentHandler.class);
        speechTranscriber = mock(SpeechTranscriber.class);
        service = new VoiceService(sessionService, classifier, router, speechTranscriber);
    }

    @Test
    void classifiesNewConversationAndSavesProgress() {
        VoiceRequest request = new VoiceRequest("u123", "내일 날씨 알려줘", null);
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("forecastDate", "2026-08-18");
        VoiceResponse response = new VoiceResponse(
                Intent.WEATHER_INFO, "ASK_LOCATION", slots,
                "어느 지역의 날씨를 알려드릴까요?", "WEATHER_INPUT", null
        );

        when(sessionService.find("u123")).thenReturn(Optional.empty());
        when(classifier.classify(request.text())).thenReturn(Intent.WEATHER_INFO);
        when(router.route(Intent.WEATHER_INFO)).thenReturn(handler);
        when(handler.handle(argThat(state -> state.step().equals("NEW")), eq(request.text()))).thenReturn(response);

        assertThat(service.process(request)).isEqualTo(response);
        verify(sessionService).save(argThat(state -> state.step().equals("ASK_LOCATION")));
        verify(sessionService, never()).clear("u123");
    }

    @Test
    void continuesExistingConversationWhenFollowUpHasNoNewIntent() {
        SessionState existing = new SessionState("u123", Intent.MAP_ROUTE, "ASK_ORIGIN", Map.of());
        VoiceResponse response = new VoiceResponse(
                Intent.MAP_ROUTE, "DONE", Map.of(),
                "길을 안내할게요.", "MAP_RESULT", Map.of("durationMinutes", 20)
        );

        when(sessionService.find("u123")).thenReturn(Optional.of(existing));
        when(classifier.classify("네")).thenReturn(Intent.UNKNOWN);
        when(router.route(Intent.MAP_ROUTE)).thenReturn(handler);
        when(handler.handle(existing, "네")).thenReturn(response);

        assertThat(service.process(new VoiceRequest("u123", "네", null))).isEqualTo(response);
        verify(classifier).classify("네");
        verify(sessionService).clear("u123");
        verify(sessionService, never()).save(any());
    }

    @Test
    void startsNewConversationWhenUserChangesIntent() {
        SessionState weatherSession = new SessionState(
                "u123", Intent.WEATHER_INFO, "ASK_LOCATION", Map.of("forecastDate", "2026-08-18")
        );
        VoiceResponse response = new VoiceResponse(
                Intent.YOUTUBE_SEARCH, "CONFIRM", Map.of("query", "아이유"),
                "아이유 검색 결과예요. 보고 싶은 영상을 골라 주세요.",
                "YOUTUBE_SEARCH_RESULT", Map.of("videos", java.util.List.of())
        );

        when(sessionService.find("u123")).thenReturn(Optional.of(weatherSession));
        when(classifier.classify("아이유 검색해줘")).thenReturn(Intent.YOUTUBE_SEARCH);
        when(router.route(Intent.YOUTUBE_SEARCH)).thenReturn(handler);
        when(handler.handle(argThat(state ->
                state.intent() == Intent.YOUTUBE_SEARCH && state.step().equals("NEW")
        ), eq("아이유 검색해줘"))).thenReturn(response);

        assertThat(service.process(new VoiceRequest("u123", "아이유 검색해줘", null))).isEqualTo(response);
        verify(sessionService).save(argThat(state -> state.intent() == Intent.YOUTUBE_SEARCH));
    }

    @Test
    void rejectsRequestWithoutTextOrAudio() {
        assertThatThrownBy(() -> service.process(new VoiceRequest("u123", " ", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void transcribesAudioBeforeClassifyingIntent() {
        VoiceRequest request = new VoiceRequest("u123", null, "YXVkaW8=");
        VoiceResponse response = new VoiceResponse(
                Intent.YOUTUBE_PLAY, "DONE", Map.of("query", "미스트롯"),
                "유튜브를 열어드릴게요.", "APP_LAUNCH", Map.of()
        );

        when(speechTranscriber.transcribe(request.audio())).thenReturn("미스트롯 영상 틀어줘");
        when(sessionService.find("u123")).thenReturn(Optional.empty());
        when(classifier.classify("미스트롯 영상 틀어줘")).thenReturn(Intent.YOUTUBE_PLAY);
        when(router.route(Intent.YOUTUBE_PLAY)).thenReturn(handler);
        when(handler.handle(any(), eq("미스트롯 영상 틀어줘"))).thenReturn(response);

        assertThat(service.process(request)).isEqualTo(response);
        verify(speechTranscriber).transcribe(request.audio());
        verify(sessionService).clear("u123");
    }

    @Test
    void passesCoordinatesToMedicalHandler() {
        VoiceRequest request = new VoiceRequest(
                "u123", "가까운 약국 찾아줘", null, 37.5665, 126.978
        );
        VoiceResponse response = new VoiceResponse(
                Intent.MEDICAL_ROUTE,
                "DONE",
                Map.of("type", "PHARMACY"),
                "주변 약국을 찾았어요.",
                "NAVER_MAP_VIEW",
                Map.of()
        );

        when(sessionService.find("u123")).thenReturn(Optional.empty());
        when(classifier.classify(request.text())).thenReturn(Intent.MEDICAL_ROUTE);
        when(router.route(Intent.MEDICAL_ROUTE)).thenReturn(handler);
        when(handler.handle(argThat(state ->
                state.slots().get("latitude").equals(37.5665)
                        && state.slots().get("longitude").equals(126.978)
        ), eq(request.text()))).thenReturn(response);

        assertThat(service.process(request)).isEqualTo(response);
        verify(sessionService).clear("u123");
    }

}
