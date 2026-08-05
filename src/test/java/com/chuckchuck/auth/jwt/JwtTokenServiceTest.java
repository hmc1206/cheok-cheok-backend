package com.chuckchuck.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import com.chuckchuck.auth.AuthProperties;

class JwtTokenServiceTest {
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(new AuthProperties(
                "test-jwt-secret-that-is-longer-than-thirty-two-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "http://localhost:5173/auth/callback",
                false
        ));
    }

    @Test
    void issuesAndValidatesTokenPair() {
        TokenPair pair = tokenService.createTokenPair("u123");

        assertThat(tokenService.accessTokenDecoder().decode(pair.accessToken()).getSubject())
                .isEqualTo("u123");
        assertThat(tokenService.requireRefreshSubject(pair.refreshToken())).isEqualTo("u123");
    }

    @Test
    void rejectsRefreshTokenAsBearerAccessToken() {
        TokenPair pair = tokenService.createTokenPair("u123");

        assertThatThrownBy(() -> tokenService.accessTokenDecoder().decode(pair.refreshToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredAccessToken() {
        JwtTokenService expiredTokenService = new JwtTokenService(
                properties(
                        "test-jwt-secret-that-is-longer-than-thirty-two-bytes",
                        Duration.ofMinutes(30)
                ),
                Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC)
        );
        String token = expiredTokenService.createAccessToken("u123");

        assertThatThrownBy(() -> expiredTokenService.accessTokenDecoder().decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtTokenService anotherTokenService = new JwtTokenService(properties(
                "another-test-secret-that-is-longer-than-thirty-two-bytes",
                Duration.ofMinutes(30)
        ));
        String forgedToken = anotherTokenService.createAccessToken("u123");

        assertThatThrownBy(() -> tokenService.accessTokenDecoder().decode(forgedToken))
                .isInstanceOf(JwtException.class);
    }

    private AuthProperties properties(String secret, Duration accessTtl) {
        return new AuthProperties(
                secret,
                accessTtl,
                Duration.ofDays(14),
                "http://localhost:5173/auth/callback",
                false
        );
    }
}
