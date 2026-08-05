package com.chuckchuck.auth.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.chuckchuck.auth.AuthProperties;
import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.jwt.TokenPair;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    public static final String REFRESH_COOKIE = "refreshToken";

    private final JwtTokenService tokenService;
    private final AuthProperties properties;

    public OAuth2LoginSuccessHandler(JwtTokenService tokenService, AuthProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuthUserPrincipal principal = (OAuthUserPrincipal) authentication.getPrincipal();
        TokenPair tokens = tokenService.createTokenPair(principal.getName());

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(properties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String redirectUrl = UriComponentsBuilder.fromUriString(properties.frontendCallbackUrl())
                .queryParam("token", tokens.accessToken())
                .queryParam("isNewUser", principal.isNewUser())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
