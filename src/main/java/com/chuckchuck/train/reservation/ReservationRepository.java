package com.chuckchuck.train.reservation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByUserIdOrderByTravelDateAsc(String userId);

    Optional<Reservation> findByReservationIdAndUserId(String reservationId, String userId);
}
