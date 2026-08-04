package com.chuckchuck.train.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.train.TrainCandidate;

@DataJpaTest
class ReservationServiceTest {
    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void storesListsAndCancelsOnlyOwnersReservation() {
        ReservationService service = new ReservationService(reservationRepository);
        TrainTicket ticket = service.reserve(
                "u123",
                Map.of(
                        "date", "2026-08-02",
                        "departure", "서울",
                        "arrival", "부산"
                ),
                new TrainCandidate("KTX-101", "14:05", "16:45", 59_800, true)
        );

        assertThat(service.findAll("u123"))
                .singleElement()
                .extracting(ReservationSummary::status)
                .isEqualTo("CONFIRMED");

        assertThatThrownBy(() -> service.cancel(ticket.reservationId(), "another-user"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        service.cancel(ticket.reservationId(), "u123");
        assertThat(service.findAll("u123"))
                .singleElement()
                .extracting(ReservationSummary::status)
                .isEqualTo("CANCELLED");
    }
}
