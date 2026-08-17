package com.chuckchuck.youtube;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class YoutubeApiClient {
    private final YoutubeClient youtubeClient;

    public YoutubeApiClient(YoutubeClient youtubeClient) {
        this.youtubeClient = youtubeClient;
    }

    public Optional<YoutubeVideo> searchFirst(String query) {
        YoutubeGoogleResponse response = youtubeClient.search(query);
        if (response == null || response.items() == null) {
            return Optional.empty();
        }

        return response.items().stream()
                .filter(item -> item != null && item.id() != null && item.id().videoId() != null)
                .filter(item -> item.snippet() != null)
                .map(item -> new YoutubeVideo(
                        item.id().videoId(),
                        item.snippet().title(),
                        thumbnailUrl(item.snippet().thumbnails()),
                        item.snippet().channelTitle(),
                        0
                ))
                .findFirst();
    }

    private String thumbnailUrl(YoutubeGoogleResponse.Thumbnails thumbnails) {
        if (thumbnails == null) return null;
        if (thumbnails.high() != null) return thumbnails.high().url();
        if (thumbnails.medium() != null) return thumbnails.medium().url();
        return thumbnails.defaultThumbnail() == null ? null : thumbnails.defaultThumbnail().url();
    }
}
