package com.chuckchuck.voice;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;
import com.chuckchuck.common.exception.GlobalExceptionHandler;

class VoiceControllerTest {
    private VoiceService voiceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        voiceService = mock(VoiceService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VoiceController(voiceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCommonVoiceEnvelope() throws Exception {
        when(voiceService.process(any())).thenReturn(new VoiceResponse(
                Intent.UNKNOWN,
                "DONE",
                Map.of(),
                "하고 싶은 일을 다시 말씀해 주세요.",
                "VOICE_INPUT",
                null
        ));

        mockMvc.perform(post("/voice/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u123","text":"도와줘"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("UNKNOWN"))
                .andExpect(jsonPath("$.step").value("DONE"))
                .andExpect(jsonPath("$.slots").isMap())
                .andExpect(jsonPath("$.ttsText").isString())
                .andExpect(jsonPath("$.screen").value("VOICE_INPUT"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsCommonErrorEnvelope() throws Exception {
        when(voiceService.process(any())).thenThrow(new ApiException(ErrorCode.INVALID_REQUEST));

        mockMvc.perform(post("/voice/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").isString())
                .andExpect(jsonPath("$.ttsText").isString());
    }
}
