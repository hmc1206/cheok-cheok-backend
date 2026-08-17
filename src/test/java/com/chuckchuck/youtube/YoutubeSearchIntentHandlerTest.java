package com.chuckchuck.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class YoutubeSearchIntentHandlerTest {

    private static final YoutubeVideoResponse VIDEO = new YoutubeVideoResponse(
            "abcd1234", "아이유 - 밤편지", "설명", "이지금", "2026-01-31T09:00:00Z", "https://img/1.jpg"
    );

    @Test
    void returnsVideoListForSearchRequest() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        when(client.search("아이유")).thenReturn(List.of(VIDEO));
        YoutubeSearchIntentHandler handler = new YoutubeSearchIntentHandler(client);

        VoiceResponse response = handler.handle(session("NEW"), "아이유 검색해줘");

        // 프론트 YoutubePlayerScreen은 intent YOUTUBE_SEARCH + step CONFIRM일 때만 목록을 그린다.
        assertThat(response.intent()).isEqualTo(Intent.YOUTUBE_SEARCH);
        assertThat(response.step()).isEqualTo("CONFIRM");
        assertThat(response.slots()).containsEntry("query", "아이유");
        assertThat(response.ttsText()).isEqualTo("아이유 검색 결과예요. 보고 싶은 영상을 골라 주세요.");
        assertThat(response.data()).isInstanceOfSatisfying(
                YoutubeSearchIntentHandler.SearchResults.class,
                data -> assertThat(data.videos()).containsExactly(VIDEO)
        );
        verify(client).search("아이유");
    }

    @Test
    void stripsCommandWordsLikeThePlayFlow() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        when(client.search("트로트")).thenReturn(List.of(VIDEO));

        VoiceResponse response = new YoutubeSearchIntentHandler(client)
                .handle(session("NEW"), "트로트 영상 찾아줘");

        assertThat(response.slots()).containsEntry("query", "트로트");
    }

    @Test
    void endsConversationWhenNothingFound() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);
        when(client.search("없는노래")).thenReturn(List.of());

        VoiceResponse response = new YoutubeSearchIntentHandler(client)
                .handle(session("NEW"), "없는노래 검색해줘");

        // DONE이어야 VoiceService가 세션을 지워 다음 발화가 검색에 묶이지 않는다.
        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.ttsText()).isEqualTo("없는노래 검색 결과가 없어요. 다른 말로 다시 말씀해 주세요.");
    }

    @Test
    void asksAgainWhenQueryIsOnlyCommandWords() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);

        VoiceResponse response = new YoutubeSearchIntentHandler(client)
                .handle(session("NEW"), "영상 검색해줘");

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.ttsText()).isEqualTo("어떤 영상을 찾아드릴지 다시 말씀해 주세요.");
        verifyNoInteractions(client);
    }

    @Test
    void doesNotSearchForBareYesAfterShowingList() {
        YoutubeApiClient client = mock(YoutubeApiClient.class);

        VoiceResponse response = new YoutubeSearchIntentHandler(client)
                .handle(session("CONFIRM"), "네");

        assertThat(response.step()).isEqualTo("DONE");
        assertThat(response.ttsText()).isEqualTo("목록에서 보고 싶은 영상을 골라 주세요.");
        verifyNoInteractions(client);
    }

    private SessionState session(String step) {
        return new SessionState("u123", Intent.YOUTUBE_SEARCH, step, Map.of());
    }
}
