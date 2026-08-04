package com.chuckchuck.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class MapIntentHandlerTest {
    private MapApiClient mapApiClient;
    private MapIntentHandler handler;

    @BeforeEach
    void setUp() {
        mapApiClient = mock(MapApiClient.class);
        handler = new MapIntentHandler(mapApiClient);
    }

    @Test
    void resolvesRegisteredAliasAndUsesCurrentLocation() {
        RouteResult route = new RouteResult(
                35,
                1,
                1_400,
                List.of(RouteStep.bus("302번 버스 탑승", 25, "행복아파트"))
        );
        when(mapApiClient.findRoute("현재위치", "경기도 성남시 분당구 행복로 10"))
                .thenReturn(Optional.of(route));

        VoiceResponse question = handler.handle(newSession(), "아들 집 가는 길 알려줘");
        assertThat(question.step()).isEqualTo("ASK_ORIGIN");
        assertThat(question.slots()).containsEntry("destinationAlias", "아들집");

        VoiceResponse result = handler.handle(
                session(question.step(), question.slots()),
                "네"
        );

        assertThat(result.step()).isEqualTo("DONE");
        assertThat(result.screen()).isEqualTo("MAP_RESULT");
        assertThat(result.slots()).containsEntry("origin", "현재위치");
        assertThat(result.data()).isEqualTo(route);
    }

    @Test
    void asksForAddressWhenAliasIsNotRegistered() {
        VoiceResponse addressQuestion = handler.handle(newSession(), "친구 집 가는 길 알려줘");

        assertThat(addressQuestion.step()).isEqualTo("ASK_ORIGIN");
        assertThat(addressQuestion.ttsText()).contains("주소가 등록되어 있지 않아요");

        VoiceResponse originQuestion = handler.handle(
                session(addressQuestion.step(), addressQuestion.slots()),
                "서울시 종로구 세종대로 1"
        );

        assertThat(originQuestion.step()).isEqualTo("ASK_ORIGIN");
        assertThat(originQuestion.slots()).containsEntry("destination", "서울시 종로구 세종대로 1");
        assertThat(originQuestion.ttsText()).contains("지금 계신 곳");
    }

    @Test
    void returnsNotFoundScreenWhenRouteIsEmpty() {
        when(mapApiClient.findRoute("서울역", "부산역")).thenReturn(Optional.empty());

        VoiceResponse response = handler.handle(
                session("ASK_ORIGIN", Map.of("destination", "부산역", "destinationAlias", "부산역")),
                "서울역"
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("MAP_NOT_FOUND");
    }

    @Test
    void removesRouteCommandFromDestination() {
        assertThat(handler.extractDestination("서울역 가는 길 알려줘")).isEqualTo("서울역");
    }

    @Test
    void rejectsUnknownSessionStep() {
        assertThatThrownBy(() -> handler.handle(session("BROKEN", Map.of()), "네"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.SESSION_EXPIRED));
    }

    private SessionState newSession() {
        return session("NEW", Map.of());
    }

    private SessionState session(String step, Map<String, Object> slots) {
        return new SessionState("u123", Intent.MAP_ROUTE, step, slots);
    }
}
