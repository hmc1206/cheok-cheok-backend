package com.chuckchuck.train.reservation;

public record TrainTicket(
        String reservationId,
        String trainNo,
        String seat,
        int price,
        String departTime,
        String arriveTime,
        String departStation,
        String arriveStation
) {
    public static TrainTicket from(Reservation reservation) {
        return new TrainTicket(
                reservation.getReservationId(),
                reservation.getTrainNo(),
                reservation.getSeat(),
                reservation.getPrice(),
                reservation.getDepartTime(),
                reservation.getArriveTime(),
                reservation.getDeparture() + "역",
                reservation.getArrival() + "역"
        );
    }
}
