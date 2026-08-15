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
}