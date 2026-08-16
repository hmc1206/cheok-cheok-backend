package com.chuckchuck.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

class IntentRouterTest {

    @Test
    void routesIntentToMatchingHandler() {
        IntentHandler handler = handler(Intent.MAP_ROUTE);
        IntentRouter router = new IntentRouter(List.of(handler));

        assertThat(router.route(Intent.MAP_ROUTE)).isSameAs(handler);
    }

    @Test
    void rejectsMissingHandler() {
        IntentRouter router = new IntentRouter(List.of());

        assertThatThrownBy(() -> router.route(Intent.MAP_ROUTE))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTENT_NOT_FOUND));
    }

    @Test
    void rejectsDuplicateHandlers() {
        assertThatThrownBy(() -> new IntentRouter(List.of(
                handler(Intent.YOUTUBE_PLAY),
                handler(Intent.YOUTUBE_PLAY)
        ))).isInstanceOf(IllegalStateException.class);
    }

    private IntentHandler handler(Intent intent) {
        IntentHandler handler = mock(IntentHandler.class);
        when(handler.supports()).thenReturn(intent);
        return handler;
    }
}
