package com.chuckchuck.map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class MapIntentHandler implements IntentHandler {
    private static final String ASK_ORIGIN = "ASK_ORIGIN";
    private static final Pattern COMMAND_WORDS = Pattern.compile(
            "가는\\s*길\\s*알려\\s*줘|가는\\s*길|길\\s*알려\\s*줘|길찾기|지도(?:에서)?|"
                    + "안내해\\s*줘|가고\\s*싶어"
    );
    private static final Map<String, String> REGISTERED_ALIASES = Map.of(
            "아들집", "경기도 성남시 분당구 행복로 10",
            "딸집", "서울특별시 송파구 사랑로 20",
            "병원", "서울특별시 강남구 건강로 30"
    );

    private final MapApiClient mapApiClient;

    public MapIntentHandler(MapApiClient mapApiClient) {
        this.mapApiClient = mapApiClient;
    }

    @Override
    public Intent supports() {
        return Intent.MAP_ROUTE;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());

        if ("NEW".equals(session.step())) {
            collectDestination(slots, extractDestination(userText));
            return askForMissingValue(slots);
        }

        if (!ASK_ORIGIN.equals(session.step())) {
            throw new ApiException(ErrorCode.SESSION_EXPIRED);
        }

        if (missing(slots, "destination")) {
            slots.put("destination", userText.trim());
            return input(slots, "지금 계신 곳에서 출발할까요?");
        }

        String origin = resolveOrigin(userText);
        if (origin == null) {
            return input(slots, "어디서 출발할까요? 장소 이름을 말씀해 주세요.");
        }
        slots.put("origin", origin);
        return complete(slots);
    }

    String extractDestination(String userText) {
        return COMMAND_WORDS.matcher(userText)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void collectDestination(Map<String, Object> slots, String destinationText) {
        String alias = destinationText.replaceAll("\\s+", "");
        slots.put("destinationAlias", alias);

        String registeredAddress = REGISTERED_ALIASES.get(alias);
        if (registeredAddress != null) {
            slots.put("destination", registeredAddress);
        } else if (!alias.endsWith("집") && !destinationText.isBlank()) {
            slots.put("destination", destinationText);
        }
    }

    private VoiceResponse askForMissingValue(Map<String, Object> slots) {
        if (missing(slots, "destination")) {
            String alias = String.valueOf(slots.getOrDefault("destinationAlias", "도착지"));
            String ttsText = alias.isBlank()
                    ? "어디로 가실 건가요?"
                    : alias + " 주소가 등록되어 있지 않아요. 도착할 주소를 말씀해 주세요.";
            return input(slots, ttsText);
        }
        return input(slots, "지금 계신 곳에서 출발할까요?");
    }

    private String resolveOrigin(String userText) {
        String text = userText.trim();
        if (containsAny(text, "네", "예", "응", "현재 위치", "여기")) {
            return "현재위치";
        }

        if (containsAny(text, "아니", "다른 곳")) {
            String explicitOrigin = text
                    .replace("아니오", "")
                    .replace("아니요", "")
                    .replace("아니", "")
                    .replace("다른 곳", "")
                    .trim();
            return explicitOrigin.isBlank() ? null : explicitOrigin;
        }
        return text.isBlank() ? null : text;
    }

    private VoiceResponse complete(Map<String, Object> slots) {
        return mapApiClient.findRoute(
                        String.valueOf(slots.get("origin")),
                        String.valueOf(slots.get("destination"))
                )
                .map(route -> new VoiceResponse(
                        Intent.MAP_ROUTE,
                        "DONE",
                        slots,
                        "약 " + route.durationMinutes() + "분 걸려요. 화면의 순서대로 이동해 주세요.",
                        "MAP_RESULT",
                        route
                ))
                .orElseGet(() -> new VoiceResponse(
                        Intent.MAP_ROUTE,
                        "DONE",
                        slots,
                        "가는 길을 찾지 못했어요. 출발지와 도착지를 다시 말씀해 주세요.",
                        "MAP_NOT_FOUND",
                        null
                ));
    }

    private VoiceResponse input(Map<String, Object> slots, String ttsText) {
        return new VoiceResponse(Intent.MAP_ROUTE, ASK_ORIGIN, slots, ttsText, "MAP_INPUT", null);
    }

    private boolean missing(Map<String, Object> slots, String key) {
        Object value = slots.get(key);
        return value == null || value.toString().isBlank();
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
