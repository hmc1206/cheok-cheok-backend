package com.chuckchuck.train.reservation;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@RestController
@RequestMapping("/api/train/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ReservationListResponse findAll(Principal principal) {
        return new ReservationListResponse(reservationService.findAll(userId(principal)));
    }

    @PostMapping("/{reservationId}/cancel")
    public CancelReservationResponse cancel(@PathVariable String reservationId, Principal principal) {
        reservationService.cancel(reservationId, userId(principal));
        return new CancelReservationResponse("SUCCESS", "예매를 취소했어요.");
    }

    private String userId(Principal principal) {
        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getName();
    }
}
