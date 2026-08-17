package com.chuckchuck.youtube;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.GlobalExceptionHandler;

class YoutubeControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new YoutubeController(new YoutubeLinkBuilder(), mock(YoutubeService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsEncodedYoutubeSearchLink() throws Exception {
        mockMvc.perform(get("/api/youtube/link").param("keyword", "아이유"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.app_url", containsString("results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0")))
                .andExpect(jsonPath("$.data.web_url", containsString("results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0")));
    }
}
