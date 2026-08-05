package com.chuckchuck.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class YoutubeIntentHandlerTest {

    @Test
    void extractsQueryAndReturnsVideo() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        YoutubeVideo video = new YoutubeVideo("abcd1234", "미스트롯", "thumbnail", "TV조선", 645);
        when(client.searchFirst("미스트롯")).thenReturn(Optional.of(video));
        YoutubeIntentHandler handler = new YoutubeIntentHandler(client);

        VoiceResponse response = handler.handle(session(), "미스트롯 영상 틀어줘");

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("YOUTUBE_PLAYER");
        assertThat(response.slots()).containsEntry("query", "미스트롯");
        assertThat(response.data()).isEqualTo(video);
    }

    @Test
    void returnsNotFoundWithoutVideoResult() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        when(client.searchFirst("없는 노래")).thenReturn(Optional.empty());
        YoutubeIntentHandler handler = new YoutubeIntentHandler(client);

        VoiceResponse response = handler.handle(session(), "없는 노래 영상 틀어줘");

        assertThat(response.step()).isEqualTo("NOT_FOUND");
        assertThat(response.data()).isNull();
    }

    @Test
    void removesServiceAndCommandWordsFromQuery() {
        YoutubeIntentHandler handler = new YoutubeIntentHandler(mock(YoutubeApiClient.class));

        assertThat(handler.extractQuery("유튜브 미스트롯 재생")).isEqualTo("미스트롯");
    }

    private SessionState session() {
        return new SessionState("u123", Intent.YOUTUBE_PLAY, "NEW", Map.of());
    }
}
