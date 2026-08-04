package com.chuckchuck.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.oauth.OAuth2LoginSuccessHandler;
import com.chuckchuck.auth.user.AppUserRepository;
import com.chuckchuck.common.exception.GlobalExceptionHandler;

import jakarta.servlet.http.Cookie;

class AuthControllerTest {
    private JwtTokenService tokenService;
    private AppUserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tokenService = mock(JwtTokenService.class);
        userRepository = mock(AppUserRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(tokenService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void refreshesAccessTokenFromCookie() throws Exception {
        when(tokenService.requireRefreshSubject("refresh-token")).thenReturn("u123");
        when(userRepository.existsById("u123")).thenReturn(true);
        when(tokenService.createAccessToken("u123")).thenReturn("new-access-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(OAuth2LoginSuccessHandler.REFRESH_COOKIE, "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"));
    }

    @Test
    void rejectsRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
