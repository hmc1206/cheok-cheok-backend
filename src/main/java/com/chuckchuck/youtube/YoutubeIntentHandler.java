package com.chuckchuck.youtube;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class YoutubeIntentHandler implements IntentHandler {
    private static final Pattern COMMAND_WORDS = Pattern.compile(
            "유튜브(?:에서)?|동영상|영상|틀어\\s*줘|틀어|재생해\\s*줘|재생해|재생|보여\\s*줘|보여"
    );

    private final YoutubeApiClient youtubeApiClient;

    public YoutubeIntentHandler(YoutubeApiClient youtubeApiClient) {
        this.youtubeApiClient = youtubeApiClient;
    }

    @Override
    public Intent supports() {
        return Intent.YOUTUBE_PLAY;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        String query = extractQuery(userText);
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());
        slots.put("query", query);

        if (query.isBlank()) {
            return notFound(slots, "어떤 영상을 찾을지 다시 말씀해 주세요.");
        }

        return youtubeApiClient.searchFirst(query)
                .map(video -> new VoiceResponse(
                        Intent.YOUTUBE_PLAY,
                        "DONE",
                        slots,
                        query + " 영상을 재생할게요.",
                        "YOUTUBE_PLAYER",
                        video
                ))
                .orElseGet(() -> notFound(slots, "해당 영상을 찾지 못했어요. 다른 검색어를 말씀해 주세요."));
    }

    String extractQuery(String userText) {
        return COMMAND_WORDS.matcher(userText)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private VoiceResponse notFound(Map<String, Object> slots, String ttsText) {
        return new VoiceResponse(
                Intent.YOUTUBE_PLAY,
                "NOT_FOUND",
                slots,
                ttsText,
                "YOUTUBE_PLAYER",
                null
        );
    }
}
