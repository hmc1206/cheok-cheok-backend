package com.chuckchuck.train;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TrainUtteranceParser {
    private static final String STATIONS = "광주송정|동대구|서울|용산|수원|대전|대구|부산|광주|목포|강릉";
    private static final Pattern STATION_PATTERN = Pattern.compile("(" + STATIONS + ")역?");
    private static final Pattern DEPARTURE_PATTERN = Pattern.compile("(" + STATIONS + ")역?에서");
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern KOREAN_DATE_PATTERN = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern KOREAN_TIME_PATTERN = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?");
    private static final Pattern COLON_TIME_PATTERN = Pattern.compile("(?:^|\\s)([01]?\\d|2[0-3]):([0-5]\\d)(?:$|\\s)");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;

    public TrainUtteranceParser(Clock clock) {
        this.clock = clock;
    }

    public Optional<String> station(String text) {
        Matcher matcher = STATION_PATTERN.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public Optional<String> departure(String text) {
        Matcher matcher = DEPARTURE_PATTERN.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public Optional<String> arrival(String text) {
        Matcher departureMatcher = DEPARTURE_PATTERN.matcher(text);
        int searchFrom = departureMatcher.find() ? departureMatcher.end() : 0;
        Matcher stationMatcher = STATION_PATTERN.matcher(text);
        return stationMatcher.find(searchFrom) ? Optional.of(stationMatcher.group(1)) : Optional.empty();
    }

    public Optional<String> date(String text) {
        LocalDate today = LocalDate.now(clock);
        if (text.contains("내일")) {
            return Optional.of(today.plusDays(1).toString());
        }
        if (text.contains("오늘")) {
            return Optional.of(today.toString());
        }

        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            try {
                return Optional.of(LocalDate.parse(isoMatcher.group(1)).toString());
            } catch (DateTimeException ignored) {
                return Optional.empty();
            }
        }

        Matcher koreanMatcher = KOREAN_DATE_PATTERN.matcher(text);
        if (koreanMatcher.find()) {
            try {
                return Optional.of(LocalDate.of(
                        today.getYear(),
                        Integer.parseInt(koreanMatcher.group(1)),
                        Integer.parseInt(koreanMatcher.group(2))
                ).toString());
            } catch (DateTimeException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<String> time(String text) {
        Matcher koreanMatcher = KOREAN_TIME_PATTERN.matcher(text);
        if (koreanMatcher.find()) {
            int hour = Integer.parseInt(koreanMatcher.group(2));
            int minute = koreanMatcher.group(3) == null ? 0 : Integer.parseInt(koreanMatcher.group(3));
            String meridiem = koreanMatcher.group(1);
            if ("오후".equals(meridiem) && hour < 12) {
                hour += 12;
            } else if ("오전".equals(meridiem) && hour == 12) {
                hour = 0;
            }
            return validTime(hour, minute);
        }

        Matcher colonMatcher = COLON_TIME_PATTERN.matcher(text);
        if (colonMatcher.find()) {
            return validTime(
                    Integer.parseInt(colonMatcher.group(1)),
                    Integer.parseInt(colonMatcher.group(2))
            );
        }
        return Optional.empty();
    }

    public boolean isPositive(String text) {
        return containsAny(text, "네", "예", "응", "좋아", "그걸로", "예매해", "예약해");
    }

    public boolean isNegative(String text) {
        return containsAny(text, "아니", "취소", "안 할래", "그만");
    }

    private Optional<String> validTime(int hour, int minute) {
        try {
            return Optional.of(LocalTime.of(hour, minute).format(TIME_FORMAT));
        } catch (DateTimeException ignored) {
            return Optional.empty();
        }
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
