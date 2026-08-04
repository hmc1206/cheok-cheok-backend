package com.chuckchuck.train.reservation;

public record ReservationSummary(
        String reservationId,
        String date,
        String departure,
        String arrival,
        String status
) {
    public static ReservationSummary from(Reservation reservation) {
        return new ReservationSummary(
                reservation.getReservationId(),
                reservation.getTravelDate(),
                reservation.getDeparture(),
                reservation.getArrival(),
                reservation.getStatus()
        );
    }
}
