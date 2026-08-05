package com.chuckchuck.train.reservation;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.train.TrainCandidate;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public TrainTicket reserve(String userId, Map<String, Object> slots, TrainCandidate candidate) {
        Reservation reservation = Reservation.create(
                userId,
                value(slots, "date"),
                value(slots, "departure"),
                value(slots, "arrival"),
                candidate
        );
        return TrainTicket.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationSummary> findAll(String userId) {
        return reservationRepository.findByUserIdOrderByTravelDateAsc(userId).stream()
                .map(ReservationSummary::from)
                .toList();
    }

    @Transactional
    public void cancel(String reservationId, String userId) {
        Reservation reservation = reservationRepository.findByReservationIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "예약내역을 찾을 수 없습니다.",
                        "예약 내역을 찾지 못했어요."
                ));
        reservation.cancel();
    }

    private String value(Map<String, Object> slots, String key) {
        return String.valueOf(slots.get(key));
    }
}
