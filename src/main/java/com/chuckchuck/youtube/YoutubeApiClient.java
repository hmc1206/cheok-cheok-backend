package com.chuckchuck.youtube;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class YoutubeApiClient {

    public Optional<YoutubeVideo> searchFirst(String query) {
        // YouTube API 키가 연결되기 전에도 프론트 흐름을 검증할 수 있도록 검색어별로 안정적인 Mock ID를 만든다.
        String videoId = UUID.nameUUIDFromBytes(query.getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "")
                .substring(0, 11);
        return Optional.of(new YoutubeVideo(
                videoId,
                query + " 추천 영상",
                "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg",
                "척척 추천",
                645
        ));
    }
}
