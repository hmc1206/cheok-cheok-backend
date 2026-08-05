package com.chuckchuck.train.reservation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.GlobalExceptionHandler;

class ReservationControllerTest {
    private ReservationService reservationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservationController(reservationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCurrentUsersReservations() throws Exception {
        when(reservationService.findAll("u123")).thenReturn(List.of(
                new ReservationSummary("RSV-001", "2026-08-02", "서울", "부산", "CONFIRMED")
        ));

        mockMvc.perform(get("/api/train/reservations").principal(principal("u123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservations[0].reservationId").value("RSV-001"));
    }

    @Test
    void cancelsOnlyWithAuthenticatedUserId() throws Exception {
        mockMvc.perform(post("/api/train/reservations/RSV-001/cancel").principal(principal("u123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(reservationService).cancel("RSV-001", "u123");
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        mockMvc.perform(get("/api/train/reservations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private Principal principal(String name) {
        return () -> name;
    }
}
