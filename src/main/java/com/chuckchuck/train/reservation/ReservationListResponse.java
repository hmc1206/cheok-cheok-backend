package com.chuckchuck.train.reservation;

import java.util.List;

public record ReservationListResponse(List<ReservationSummary> reservations) {
}
