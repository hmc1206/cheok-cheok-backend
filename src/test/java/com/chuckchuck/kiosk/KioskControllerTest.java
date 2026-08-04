package com.chuckchuck.kiosk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.GlobalExceptionHandler;

class KioskControllerTest {
    private KioskService kioskService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        kioskService = mock(KioskService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new KioskController(kioskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsActionResult() throws Exception {
        when(kioskService.action(eq("kiosk_cafe"), any())).thenReturn(new KioskActionResponse(
                "WRONG", 1, 5, null, null, "다시 해볼까요?", null, null, null, "hint.png"
        ));

        mockMvc.perform(post("/api/kiosk/scenarios/kiosk_cafe/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"ks_001","tappedElementId":"wrong"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("WRONG"))
                .andExpect(jsonPath("$.retryHintImageUrl").value("hint.png"));
    }
}
