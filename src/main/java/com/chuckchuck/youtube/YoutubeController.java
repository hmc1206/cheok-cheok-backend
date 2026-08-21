package com.chuckchuck.youtube;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/youtube")
public class YoutubeController {

    private final YoutubeLinkBuilder linkBuilder;
    private final YoutubeService youtubeService;

    public YoutubeController(
            YoutubeLinkBuilder linkBuilder,
            YoutubeService youtubeService
    ) {
        this.linkBuilder = linkBuilder;
        this.youtubeService = youtubeService;
    }

    @PostMapping("/play")
    public YoutubePlayResponse play(
            @RequestBody YoutubePlayRequest request
    ) {
        return youtubeService.play(request.query());
    }

    @GetMapping("/link")
    public YoutubeLinkApiResponse link(
            @RequestParam(required = false) String keyword
    ) {
        var links = (keyword == null || keyword.isBlank())
                ? new YoutubeLinkBuilder.YoutubeLinks(
                "vnd.youtube://www.youtube.com",
                "https://www.youtube.com"
        )
                : linkBuilder.forSearch(keyword);

        return new YoutubeLinkApiResponse(
                true,
                new YoutubeLinkApiResponse.Data(
                        links.webUrl(),
                        links.appUrl()
                )
        );
    }

    @PostMapping("/search")
    public YoutubeSearchListResponse search(
            @RequestBody YoutubeSearchRequest request
    ) {
        return youtubeService.search(request.keyword());
    }

    public record YoutubeSearchRequest(
            String keyword
    ) {
    }

    public record YoutubeLinkApiResponse(
            boolean success,
            Data data
    ) {
        public record Data(
                String web_url,
                String app_url
        ) {
        }
    }
}