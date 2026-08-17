package com.chuckchuck.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class YoutubeIntentHandlerTest {

    @Test
    void returnsSearchResultLinkWithoutSelectingVideo() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        YoutubeIntentHandler handler = new YoutubeIntentHandler(client, new YoutubeLinkBuilder());

        VoiceResponse response = handler.handle(session(), "아이유 검색해줘");

        assertThat(response.intent()).isEqualTo(Intent.YOUTUBE_PLAY);
        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.screen()).isEqualTo("APP_LAUNCH");
        assertThat(response.slots())
                .containsEntry("query", "아이유")
                .containsEntry("action", "SEARCH");
        assertThat(response.ttsText()).isEqualTo("아이유 검색 결과를 보여드릴게요.");
        assertThat(response.quickReplies()).isNull();
        assertThat(response.data()).isInstanceOfSatisfying(YoutubeLinkResult.class, data -> {
            assertThat(data.appUrl()).contains("/results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0");
            assertThat(data.webUrl()).contains("/results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0");
        });
        verifyNoInteractions(client);
    }

    @Test
    void returnsConfirmationForPlayRequest() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        YoutubeVideo video = new YoutubeVideo("abcd1234", "미스트롯", "thumbnail", "TV조선", 645);
        when(client.searchFirst("미스트롯")).thenReturn(Optional.of(video));
        YoutubeIntentHandler handler = new YoutubeIntentHandler(client, new YoutubeLinkBuilder());

        VoiceResponse response = handler.handle(session(), "미스트롯 영상 틀어줘");

        assertThat(response.step()).isEqualTo("CONFIRM");
        assertThat(response.screen()).isEqualTo("APP_LAUNCH");
        assertThat(response.slots())
                .containsEntry("query", "미스트롯")
                .containsEntry("action", "PLAY")
                .containsEntry("videoId", "abcd1234");
        assertThat(response.ttsText()).isEqualTo("미스트롯 영상을 열어드릴까요?");
        assertThat(response.quickReplies()).extracting("value").containsExactly("네", "아니요");
        assertThat(response.data()).isInstanceOfSatisfying(YoutubeLinkResult.class,
                data -> assertThat(data.webUrl()).endsWith("watch?v=abcd1234"));
        verify(client).searchFirst("미스트롯");
    }

    @Test
    void opensSelectedVideoAfterConfirmation() {
        YoutubeIntentHandler handler = new YoutubeIntentHandler(
                mock(YoutubeApiClient.class), new YoutubeLinkBuilder()
        );

        VoiceResponse response = handler.handle(
                new SessionState(
                        "u123",
                        Intent.YOUTUBE_PLAY,
                        "CONFIRM",
                        Map.of("query", "미스트롯", "action", "PLAY", "videoId", "abcd1234")
                ),
                "네"
        );

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.ttsText()).isEqualTo("유튜브를 열어드릴게요.");
        assertThat(response.data()).isInstanceOfSatisfying(YoutubeLinkResult.class,
                data -> assertThat(data.appUrl()).endsWith("watch?v=abcd1234"));
    }

    @Test
    void fallsBackToSearchResultsWhenVideoIsMissing() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        when(client.searchFirst("없는 노래")).thenReturn(Optional.empty());
        YoutubeIntentHandler handler = new YoutubeIntentHandler(client, new YoutubeLinkBuilder());

        VoiceResponse response = handler.handle(session(), "없는 노래 영상 틀어줘");

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.slots()).containsEntry("action", "SEARCH");
        assertThat(response.data()).isInstanceOf(YoutubeLinkResult.class);
    }

    private SessionState session() {
        return new SessionState("u123", Intent.YOUTUBE_PLAY, "NEW", Map.of());
    }
}
