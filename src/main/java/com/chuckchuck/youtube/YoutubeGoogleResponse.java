package com.chuckchuck.youtube;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YoutubeGoogleResponse(
        String kind,
        String etag,
        String nextPageToken,
        String regionCode,
        PageInfo pageInfo,
        List<Item> items
) {

    public record PageInfo(
            int totalResults,
            int resultsPerPage
    ) {
    }

    public record Item(
            String kind,
            String etag,
            Id id,
            Snippet snippet
    ) {
    }

    public record Id(
            String kind,
            String videoId
    ) {
    }

    public record Snippet(
            String publishedAt,
            String channelId,
            String title,
            String description,
            Thumbnails thumbnails,
            String channelTitle,
            String liveBroadcastContent,
            String publishTime
    ) {
    }

    public record Thumbnails(
            @JsonProperty("default")
            Thumbnail defaultThumbnail,

            Thumbnail medium,

            Thumbnail high
    ) {
    }

    public record Thumbnail(
            String url,
            int width,
            int height
    ) {
    }
}