package com.chuckchuck.youtube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YoutubeClient {

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeClient(
            RestClient.Builder restClientBuilder,
            @Value("${youtube.api-key}") String apiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://www.googleapis.com")
                .build();

        this.apiKey = apiKey;
    }

    public YoutubeGoogleResponse search(String keyword) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", keyword)
                        .queryParam("type", "video")
                        .queryParam("maxResults", 10)
                        .queryParam("regionCode", "KR")
                        .queryParam("key", apiKey)
                        .build()
                )
                .retrieve()
                .body(YoutubeGoogleResponse.class);
    }
}