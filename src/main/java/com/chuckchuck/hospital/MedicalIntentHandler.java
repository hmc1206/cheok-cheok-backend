package com.chuckchuck.hospital;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.QuickReply;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class MedicalIntentHandler implements IntentHandler {
    private static final String HOSPITAL = "HOSPITAL";
    private static final String PHARMACY = "PHARMACY";

    private final MapService mapService;

    public MedicalIntentHandler(MapService mapService) {
        this.mapService = mapService;
    }

    @Override
    public Intent supports() {
        return Intent.MEDICAL_ROUTE;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());
        String type = resolveType(slots, userText);
        slots.put("type", type);

        Double latitude = number(slots.get("latitude"));
        Double longitude = number(slots.get("longitude"));
        if (latitude == null || longitude == null) {
            String facility = PHARMACY.equals(type) ? "약국" : "병원";
            return new VoiceResponse(
                    Intent.MEDICAL_ROUTE,
                    "ASK_LOCATION",
                    slots,
                    "가까운 " + facility + "을 찾으려면 현재 위치가 필요해요. 위치 권한을 허용해 주세요.",
                    "MEDICAL_INPUT",
                    List.of(new QuickReply("현재 위치 사용", "현재 위치")),
                    null
            );
        }

        MedicalRouteResponseDto result = mapService.processMedicalRoute(
                new MedicalRouteRequestDto(session.userId(), userText, type, latitude, longitude)
        );
        return new VoiceResponse(
                Intent.MEDICAL_ROUTE,
                "DONE",
                Map.of("type", type),
                result.ttsText(),
                "NAVER_MAP_VIEW",
                result.data()
        );
    }

    private String resolveType(Map<String, Object> slots, String userText) {
        if (userText.contains("약국")) {
            return PHARMACY;
        }
        if (userText.contains("병원") || userText.contains("의원") || userText.contains("응급실")) {
            return HOSPITAL;
        }
        Object savedType = slots.get("type");
        return PHARMACY.equals(savedType) ? PHARMACY : HOSPITAL;
    }

    private Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
