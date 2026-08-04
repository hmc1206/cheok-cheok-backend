package com.chuckchuck.train;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chuckchuck.session.SessionState;
import com.chuckchuck.train.reservation.ReservationService;
import com.chuckchuck.train.reservation.TrainTicket;
import com.chuckchuck.voice.Intent;
import com.chuckchuck.voice.VoiceResponse;

class TrainIntentHandlerTest {
    private TrainApiClient trainApiClient;
    private ReservationService reservationService;
    private TrainIntentHandler handler;

    @BeforeEach
    void setUp() {
        trainApiClient = mock(TrainApiClient.class);
        reservationService = mock(ReservationService.class);
        TrainUtteranceParser parser = new TrainUtteranceParser(
                Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
        handler = new TrainIntentHandler(parser, trainApiClient, reservationService);
    }

    @Test
    void completesBookingStateMachine() {
        TrainCandidate candidate = new TrainCandidate("KTX-101", "14:05", "16:45", 59_800, true);
        when(trainApiClient.search(any())).thenReturn(List.of(candidate));

        VoiceResponse departureQuestion = handle("NEW", Map.of(), "부산 가는 기차표 끊어줘");
        assertThat(departureQuestion.step()).isEqualTo("ASK_DEPARTURE");
        assertThat(departureQuestion.slots()).containsEntry("arrival", "부산");

        VoiceResponse dateQuestion = handle(
                departureQuestion.step(), departureQuestion.slots(), "서울역"
        );
        assertThat(dateQuestion.step()).isEqualTo("ASK_DATE");

        VoiceResponse timeQuestion = handle(dateQuestion.step(), dateQuestion.slots(), "내일");
        assertThat(timeQuestion.step()).isEqualTo("ASK_TIME");
        assertThat(timeQuestion.slots()).containsEntry("date", "2026-08-02");

        VoiceResponse confirmation = handle(timeQuestion.step(), timeQuestion.slots(), "오후 2시");
        assertThat(confirmation.step()).isEqualTo("CONFIRM");
        assertThat(confirmation.screen()).isEqualTo("TRAIN_CONFIRM");

        TrainTicket ticket = new TrainTicket(
                "RSV-001", "KTX-101", "5호차 12A", 59_800,
                "14:05", "16:45", "서울역", "부산역"
        );
        when(reservationService.reserve(eq("u123"), any(), eq(candidate))).thenReturn(ticket);

        VoiceResponse completed = handle(confirmation.step(), confirmation.slots(), "그걸로 예매해줘");

        assertThat(completed.step()).isEqualTo("DONE");
        assertThat(completed.screen()).isEqualTo("TRAIN_TICKET");
        assertThat(completed.data()).isEqualTo(ticket);
        verify(reservationService).reserve(eq("u123"), any(), eq(candidate));
    }

    @Test
    void keepsConfirmationWhenAnswerIsUnclear() {
        TrainCandidate candidate = new TrainCandidate("KTX-101", "14:05", "16:45", 59_800, true);
        when(trainApiClient.search(any())).thenReturn(List.of(candidate));

        VoiceResponse response = handle(
                "CONFIRM",
                Map.of(
                        "departure", "서울", "arrival", "부산",
                        "date", "2026-08-02", "time", "14:00", "trainNo", "KTX-101"
                ),
                "글쎄"
        );

        assertThat(response.step()).isEqualTo("CONFIRM");
        assertThat(response.ttsText()).contains("네 또는 아니오");
    }

    private VoiceResponse handle(String step, Map<String, Object> slots, String text) {
        return handler.handle(new SessionState("u123", Intent.TRAIN_BOOKING, step, slots), text);
    }
}
