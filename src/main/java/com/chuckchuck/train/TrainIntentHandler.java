package com.chuckchuck.train;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.session.SessionState;
import com.chuckchuck.train.reservation.ReservationService;
import com.chuckchuck.train.reservation.TrainTicket;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.IntentHandler;
import com.chuckchuck.voice.VoiceResponse;

@Component
public class TrainIntentHandler implements IntentHandler {
    private static final String ASK_DEPARTURE = "ASK_DEPARTURE";
    private static final String ASK_DATE = "ASK_DATE";
    private static final String ASK_TIME = "ASK_TIME";
    private static final String CONFIRM = "CONFIRM";
    private static final String DONE = "DONE";

    private final TrainUtteranceParser parser;
    private final TrainApiClient trainApiClient;
    private final ReservationService reservationService;

    public TrainIntentHandler(
            TrainUtteranceParser parser,
            TrainApiClient trainApiClient,
            ReservationService reservationService
    ) {
        this.parser = parser;
        this.trainApiClient = trainApiClient;
        this.reservationService = reservationService;
    }

    @Override
    public Intent supports() {
        return Intent.TRAIN_BOOKING;
    }

    @Override
    public VoiceResponse handle(SessionState session, String userText) {
        Map<String, Object> slots = initializedSlots(session.slots());

        if (CONFIRM.equals(session.step())) {
            return confirm(session.userId(), slots, userText);
        }

        collectSlots(session.step(), slots, userText);
        return next(slots);
    }

    private void collectSlots(String step, Map<String, Object> slots, String userText) {
        switch (step) {
            case "NEW" -> {
                parser.departure(userText).ifPresent(value -> slots.put("departure", value));
                parser.arrival(userText).ifPresent(value -> slots.put("arrival", value));
                parser.date(userText).ifPresent(value -> slots.put("date", value));
                parser.time(userText).ifPresent(value -> slots.put("time", value));
            }
            case ASK_DEPARTURE -> parser.station(userText).ifPresent(value -> {
                // 계약에 ASK_ARRIVAL이 없으므로 목적지가 비어 있으면 같은 단계에서 목적지를 먼저 채운다.
                String key = missing(slots, "arrival") ? "arrival" : "departure";
                slots.put(key, value);
            });
            case ASK_DATE -> {
                parser.date(userText).ifPresent(value -> slots.put("date", value));
                parser.time(userText).ifPresent(value -> slots.put("time", value));
            }
            case ASK_TIME -> parser.time(userText).ifPresent(value -> slots.put("time", value));
            default -> throw new ApiException(ErrorCode.SESSION_EXPIRED);
        }
    }

    private VoiceResponse next(Map<String, Object> slots) {
        if (missing(slots, "arrival")) {
            return input(ASK_DEPARTURE, slots, "어디로 가실 건가요?");
        }
        if (missing(slots, "departure")) {
            return input(ASK_DEPARTURE, slots, "어디서 출발하시나요?");
        }
        if (missing(slots, "date")) {
            return input(ASK_DATE, slots, "언제 출발하시나요? 오늘이나 내일 중 말씀해 주세요.");
        }
        if (missing(slots, "time")) {
            return input(ASK_TIME, slots, "몇 시에 출발하시겠어요?");
        }

        List<TrainCandidate> candidates = trainApiClient.search(slots).stream()
                .filter(TrainCandidate::seatAvailable)
                .toList();
        if (candidates.isEmpty()) {
            return new VoiceResponse(
                    Intent.TRAIN_BOOKING,
                    DONE,
                    slots,
                    "조건에 맞는 열차를 찾지 못했어요. 시간을 바꿔 다시 말씀해 주세요.",
                    "TRAIN_INPUT",
                    null
            );
        }

        TrainCandidate selected = candidates.getFirst();
        slots.put("trainNo", selected.trainNo());
        return new VoiceResponse(
                Intent.TRAIN_BOOKING,
                CONFIRM,
                slots,
                formatConfirmation(selected),
                "TRAIN_CONFIRM",
                Map.of("candidates", candidates)
        );
    }

    private VoiceResponse confirm(String userId, Map<String, Object> slots, String userText) {
        if (parser.isNegative(userText)) {
            return new VoiceResponse(
                    Intent.TRAIN_BOOKING,
                    DONE,
                    slots,
                    "기차 예매를 취소했어요.",
                    "VOICE_INPUT",
                    null
            );
        }

        List<TrainCandidate> candidates = trainApiClient.search(slots);
        TrainCandidate selected = candidates.stream()
                .filter(candidate -> candidate.trainNo().equals(slots.get("trainNo")))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.EXTERNAL_API_FAIL));

        if (!parser.isPositive(userText)) {
            return new VoiceResponse(
                    Intent.TRAIN_BOOKING,
                    CONFIRM,
                    slots,
                    "이 열차로 예매할까요? 네 또는 아니오로 말씀해 주세요.",
                    "TRAIN_CONFIRM",
                    Map.of("candidates", candidates)
            );
        }

        TrainTicket ticket = reservationService.reserve(userId, slots, selected);
        return new VoiceResponse(
                Intent.TRAIN_BOOKING,
                DONE,
                slots,
                formatTicketMessage(slots, ticket),
                "TRAIN_TICKET",
                ticket
        );
    }

    private VoiceResponse input(String step, Map<String, Object> slots, String ttsText) {
        return new VoiceResponse(Intent.TRAIN_BOOKING, step, slots, ttsText, "TRAIN_INPUT", null);
    }

    private Map<String, Object> initializedSlots(Map<String, Object> existing) {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("departure", null);
        slots.put("arrival", null);
        slots.put("date", null);
        slots.put("time", null);
        slots.putAll(existing);
        return slots;
    }

    private boolean missing(Map<String, Object> slots, String key) {
        Object value = slots.get(key);
        return value == null || value.toString().isBlank();
    }

    private String formatConfirmation(TrainCandidate candidate) {
        return koreanTime(candidate.departTime()) + "에 출발하는 KTX 열차가 있어요. 예매할까요?";
    }

    private String formatTicketMessage(Map<String, Object> slots, TrainTicket ticket) {
        LocalDate date = LocalDate.parse((String) slots.get("date"));
        return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일 "
                + koreanTime(ticket.departTime()) + "에 " + ticket.departStation()
                + "에서 출발하는 KTX 예매가 완료됐어요.";
    }

    private String koreanTime(String value) {
        LocalTime time = LocalTime.parse(value);
        String meridiem = time.getHour() < 12 ? "오전" : "오후";
        int hour = time.getHour() % 12;
        if (hour == 0) {
            hour = 12;
        }
        return time.getMinute() == 0
                ? meridiem + " " + hour + "시"
                : meridiem + " " + hour + "시 " + time.getMinute() + "분";
    }
}
