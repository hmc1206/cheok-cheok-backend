package com.chuckchuck.youtube;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class YoutubeService {

    private final YoutubeClient youtubeClient;

    public YoutubeService(YoutubeClient youtubeClient) {
        this.youtubeClient = youtubeClient;
    }

    public YoutubeSearchListResponse search(String keyword) {

        // 1. 유튜브 API 검색 호출
        YoutubeGoogleResponse response = youtubeClient.search(keyword);

        // 2. 검색 데이터 가공하여 영상 리스트(List) 생성
        List<YoutubeVideoResponse> videoList = response.items().stream()
                .filter(item -> item.id() != null)
                .filter(item -> item.id().videoId() != null)
                .map(YoutubeVideoResponse::from)
                .toList();

        // 3. 첫 번째 대화에서 요구한 JSON 포맷 구조로 최종 조립하여 반환
        return new YoutubeSearchListResponse(
                "YOUTUBE_SEARCH",                                 // intent
                "CONFIRM",                                        // step
                new YoutubeSearchListResponse.Slots(keyword),     // slots
                keyword + " 영상 검색목록입니다",                     // ttsText
                "APP_LAUNCH",                                     // screen
                new YoutubeSearchListResponse.Data(videoList)     // data (가공한 리스트 래핑)
        );
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