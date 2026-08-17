package com.chuckchuck.youtube;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class YoutubeApiClientTest {

    @Test
    void mapsFirstGoogleSearchResult() {
        YoutubeClient youtubeClient = mock(YoutubeClient.class);
        YoutubeGoogleResponse response = new YoutubeGoogleResponse(
                null,
                null,
                null,
                "KR",
                null,
                List.of(new YoutubeGoogleResponse.Item(
                        null,
                        null,
                        new YoutubeGoogleResponse.Id("youtube#video", "abcd1234"),
                        new YoutubeGoogleResponse.Snippet(
                                null,
                                null,
                                "아이유 좋은 날",
                                null,
                                new YoutubeGoogleResponse.Thumbnails(
                                        null,
                                        null,
                                        new YoutubeGoogleResponse.Thumbnail("thumbnail", 480, 360)
                                ),
                                "아이유 공식 채널",
                                null,
                                null
                        )
                ))
        );
        when(youtubeClient.search("아이유")).thenReturn(response);

        YoutubeVideo video = new YoutubeApiClient(youtubeClient).searchFirst("아이유").orElseThrow();

        assertThat(video.videoId()).isEqualTo("abcd1234");
        assertThat(video.title()).isEqualTo("아이유 좋은 날");
        assertThat(video.thumbnailUrl()).isEqualTo("thumbnail");
        assertThat(video.channelName()).isEqualTo("아이유 공식 채널");
    }
}
