package com.chuckchuck.youtube;

import java.util.List;

public record YoutubeSearchResponse(
        List<YoutubeVideoResponse> videos,
        String nextPageToken
) {
}