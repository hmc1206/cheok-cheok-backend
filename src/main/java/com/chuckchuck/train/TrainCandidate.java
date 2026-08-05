package com.chuckchuck.train;

public record TrainCandidate(
        String trainNo,
        String departTime,
        String arriveTime,
        int price,
        boolean seatAvailable
) {
}
