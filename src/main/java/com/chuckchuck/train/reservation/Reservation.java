package com.chuckchuck.train.reservation;

import java.util.UUID;

import com.chuckchuck.train.TrainCandidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "train_reservations")
public class Reservation {
    @Id
    @Column(length = 40)
    private String reservationId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String travelDate;

    @Column(nullable = false)
    private String departure;

    @Column(nullable = false)
    private String arrival;

    @Column(nullable = false)
    private String trainNo;

    @Column(nullable = false)
    private String seat;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String departTime;

    @Column(nullable = false)
    private String arriveTime;

    @Column(nullable = false)
    private String status;

    protected Reservation() {
    }

    private Reservation(
            String userId,
            String travelDate,
            String departure,
            String arrival,
            TrainCandidate candidate
    ) {
        this.reservationId = "RSV-" + UUID.randomUUID();
        this.userId = userId;
        this.travelDate = travelDate;
        this.departure = departure;
        this.arrival = arrival;
        this.trainNo = candidate.trainNo();
        this.seat = "5호차 12A";
        this.price = candidate.price();
        this.departTime = candidate.departTime();
        this.arriveTime = candidate.arriveTime();
        this.status = "CONFIRMED";
    }

    public static Reservation create(
            String userId,
            String travelDate,
            String departure,
            String arrival,
            TrainCandidate candidate
    ) {
        return new Reservation(userId, travelDate, departure, arrival, candidate);
    }

    public void cancel() {
        status = "CANCELLED";
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public String getDeparture() {
        return departure;
    }

    public String getArrival() {
        return arrival;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public String getSeat() {
        return seat;
    }

    public int getPrice() {
        return price;
    }

    public String getDepartTime() {
        return departTime;
    }

    public String getArriveTime() {
        return arriveTime;
    }

    public String getStatus() {
        return status;
    }
}
