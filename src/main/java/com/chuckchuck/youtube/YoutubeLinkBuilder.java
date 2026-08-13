package com.chuckchuck.youtube;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Component
public class YoutubeLinkBuilder {
    //특정 영상이 확정된 경우
    public YoutubeLinks forVideo(String videoId) {
        return new YoutubeLinks(
                "vnd.youtube://www.youtube.com/watch?v=" + videoId,
                "https://www.youtube.com/watch?v=" + videoId
        );
    }

    //특정 영상을 못 찾아 검색결과로 보낼 경우
    public YoutubeLinks forSearch(String keyword){
        String encoded = encode(keyword);
        return new YoutubeLinks(
                "vnd.youtube://www.youtube.com/results?search_query=" + encoded,
                "https://www.youtube.com/results?search_query=" + encoded
        );
    }

    private String encode(String value){
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+","%2G");
    }

    public record YoutubeLinks(String appUrl, String webUrl){

    }
}
