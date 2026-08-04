package com.chuckchuck.youtube;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.GlobalExceptionHandler;

class YoutubeControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new YoutubeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void acceptsSupportedControlAction() throws Exception {
        mockMvc.perform(post("/api/youtube/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u123","action":"PAUSE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.ttsText").value("일시정지했어요."));
    }

    @Test
    void rejectsUnsupportedControlAction() throws Exception {
        mockMvc.perform(post("/api/youtube/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u123","action":"STOP"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }
}
