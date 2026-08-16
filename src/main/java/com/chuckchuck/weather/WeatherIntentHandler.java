package com.chuckchuck.weather;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.QuickReply;
import com.chuckchuck.voice.VoiceResponse;

/**
 * WEATHER_INFO 대화를 NEW → ASK_LOCATION → DONE 흐름으로 처리한다.
 */
@Component
public class WeatherIntentHandler implements IntentHandler {
    private static final String ASK_LOCATION = "ASK_LOCATION";
    private static final String DONE = "DONE";
    // 날짜와 명령 표현을 지워 "내일 서울 날씨 알려줘"에서 조회할 지역명 "서울"만 남긴다.
    private static final Pattern COMMAND_WORDS = Pattern.compile(
            "오늘|내일|모레|현재\\s*위치|지금\\s*있는\\s*곳|여기|날씨|기온|온도|"
                    + "비\\s*(?:가\\s*)?(?:와|오니|오나요|올까|와요)|눈\\s*(?:이\\s*)?(?:와|오니|오나요|올까|와요)|"
                    + "우산|가져가야\\s*해|챙겨야\\s*해|알려\\s*줘|알려줘|어때|확인해\\s*줘|확인해줘|"
                    + "지역\\s*직접\\s*입력|[?!.]"
    );

    private final WeatherService weatherService;

    public WeatherIntentHandler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Override
    public Intent supports() {
        return Intent.WEATHER_INFO;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = new LinkedHashMap<>(session.slots());
        LocalDate date = resolveDate(slots, userText);
        slots.put("forecastDate", date.toString());

        String location = extractLocation(userText);
        // 좌표는 VoiceService가 기존 IntentHandler 계약을 바꾸지 않고 전달하기 위해 슬롯에 넣는다.
        Double latitude = number(slots.get("latitude"));
        Double longitude = number(slots.get("longitude"));

        if (location.isBlank() && (latitude == null || longitude == null)) {
            return askLocation(slots);
        }

        WeatherData data = weatherService.lookup(
                location.isBlank() ? null : location,
                latitude,
                longitude,
                date
        );
        // 외부 조회에만 필요한 내부 좌표는 최종 응답 슬롯에 노출하지 않는다.
        slots.clear();
        slots.put("location", data.location().name());
        slots.put("forecastDate", data.forecastDate().toString());
        return new VoiceResponse(
                Intent.WEATHER_INFO,
                DONE,
                slots,
                ttsText(data),
                "WEATHER_RESULT",
                data
        );
    }

    String extractLocation(String userText) {
        return COMMAND_WORDS.matcher(userText)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceFirst("(?:에서|에는|에|의)$", "")
                .trim();
    }

    private LocalDate resolveDate(Map<String, Object> slots, String userText) {
        LocalDate today = weatherService.today();
        if (userText.contains("모레")) return today.plusDays(2);
        if (userText.contains("내일")) return today.plusDays(1);
        if (userText.contains("오늘")) return today;

        // 지역을 다시 물어보는 동안에도 첫 발화에서 요청한 날짜를 잃지 않는다.
        Object savedDate = slots.get("forecastDate");
        return savedDate == null ? today : weatherService.parseDate(savedDate.toString());
    }

    private VoiceResponse askLocation(Map<String, Object> slots) {
        return new VoiceResponse(
                Intent.WEATHER_INFO,
                ASK_LOCATION,
                slots,
                "어느 지역의 날씨를 알려드릴까요?",
                "WEATHER_INPUT",
                List.of(
                        new QuickReply("현재 위치 날씨", "현재 위치"),
                        new QuickReply("지역 직접 말하기", "지역 직접 입력")
                ),
                null
        );
    }

    private String ttsText(WeatherData data) {
        String date = dateLabel(data.forecastDate());
        StringBuilder text = new StringBuilder()
                .append(date).append(' ')
                .append(data.location().name()).append(" 날씨는 ")
                .append(withCopula(data.conditionText())).append(". ");
        if (data.currentTemperature() != null) {
            text.append("현재 ").append(Math.round(data.currentTemperature())).append("도, ");
        }
        if (data.maximumTemperature() != null && data.minimumTemperature() != null) {
            text.append("최고 ").append(Math.round(data.maximumTemperature()))
                    .append("도, 최저 ").append(Math.round(data.minimumTemperature())).append("도예요. ");
        }
        if (data.precipitationProbability() != null) {
            text.append("비나 눈이 올 확률은 ").append(data.precipitationProbability()).append("퍼센트예요. ");
        }
        return text.append(data.advice()).toString();
    }

    private String dateLabel(LocalDate date) {
        LocalDate today = weatherService.today();
        if (date.equals(today)) return "오늘";
        if (date.equals(today.plusDays(1))) return "내일";
        if (date.equals(today.plusDays(2))) return "모레";
        return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    // 받침 유무를 확인해 "맑음이에요", "비예요"처럼 자연스러운 TTS 문장을 만든다.
    private String withCopula(String value) {
        char last = value.charAt(value.length() - 1);
        boolean hasFinalConsonant = last >= '가' && last <= '힣' && (last - '가') % 28 != 0;
        return value + (hasFinalConsonant ? "이에요" : "예요");
    }

    private Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
