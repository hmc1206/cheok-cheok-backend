package com.chuckchuck.youtube;

public record YoutubeVideoResponse(
        String videoId,
        String title,
        String description,
        String channelTitle,
        String publishedAt,
        String thumbnailUrl
) {

    public static YoutubeVideoResponse from(
            YoutubeGoogleResponse.Item item
    ) {

        return new YoutubeVideoResponse(
                item.id().videoId(),
                item.snippet().title(),
                item.snippet().description(),
                item.snippet().channelTitle(),
                item.snippet().publishedAt(),
                item.snippet().thumbnails().high().url()
        );
    }
}