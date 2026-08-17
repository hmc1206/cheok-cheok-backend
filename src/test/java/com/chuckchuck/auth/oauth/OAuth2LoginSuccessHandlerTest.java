package com.chuckchuck.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.chuckchuck.auth.AuthProperties;
import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.jwt.TokenPair;
import com.chuckchuck.auth.user.AppUser;

class OAuth2LoginSuccessHandlerTest {

    @Test
    void redirectsWithAccessTokenAndStoresRefreshTokenInHttpOnlyCookie() throws Exception {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        AppUser user = AppUser.create("google-123", "hong@gmail.com", "홍길동", null);
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getAttributes()).thenReturn(Map.of("sub", "google-123"));
        OAuthUserPrincipal principal = new OAuthUserPrincipal(user, oidcUser, true);
        when(tokenService.createTokenPair(user.getId())).thenReturn(new TokenPair("access-token", "refresh-token"));
        AuthProperties properties = new AuthProperties(
                "test-jwt-secret-that-is-longer-than-thirty-two-bytes",
                Duration.ofMinutes(30),
                Duration.ofDays(14),
                "http://localhost:5173/auth/callback",
                false
        );
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(tokenService, properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThat(response.getRedirectedUrl())
                .contains("token=access-token")
                .contains("isNewUser=true");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("refreshToken=refresh-token")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
