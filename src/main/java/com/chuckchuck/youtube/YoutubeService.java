package com.chuckchuck.youtube;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class YoutubeService {

    private final YoutubeClient youtubeClient;

    public YoutubeService(YoutubeClient youtubeClient) {
        this.youtubeClient = youtubeClient;
    }

    public List<YoutubeVideoResponse> search(String keyword) {

        YoutubeGoogleResponse response =
                youtubeClient.search(keyword);

        return response.items().stream()
                .filter(item -> item.id() != null)
                .filter(item -> item.id().videoId() != null)
                .map(YoutubeVideoResponse::from)
                .toList();
    }

    public YoutubePlayResponse play(String query) {

        YoutubeGoogleResponse response =
                youtubeClient.search(query);

        YoutubeGoogleResponse.Item item =
                response.items().stream()
                        .filter(result -> result.id() != null)
                        .filter(result -> result.id().videoId() != null)
                        .findFirst()
                        .orElseThrow();

        String videoId = item.id().videoId();

        return new YoutubePlayResponse(
                "YOUTUBE_PLAY",
                "CONFIRM",
                new YoutubePlayResponse.Slots(query),
                query + " 영상을 열어드릴까요?",
                "APP_LAUNCH",
                List.of(
                        new YoutubePlayResponse.QuickReply(
                                "네, 열어줘",
                                "네"
                        ),
                        new YoutubePlayResponse.QuickReply(
                                "아니요",
                                "아니요"
                        )
                ),
                new YoutubePlayResponse.Data(
                        item.snippet().title(),
                        item.snippet().thumbnails().high().url(),
                        item.snippet().channelTitle(),
                        "vnd.youtube://www.youtube.com/watch?v=" + videoId,
                        "https://www.youtube.com/watch?v=" + videoId
                )
        );
    }
}