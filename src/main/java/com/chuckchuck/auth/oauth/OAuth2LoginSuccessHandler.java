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

    public OAuth2LoginSuccessHandler(
            JwtTokenService tokenService,
            AuthProperties properties
    ) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // CustomOAuth2UserService에서 만들어진 사용자
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof OAuthUserPrincipal userPrincipal)) {
            throw new ServletException(
                    "지원하지 않는 인증 Principal 타입입니다: "
                            + principal.getClass().getName()
            );
        }

        // AppUser.id(UUID)를 JWT subject로 사용
        String userId = userPrincipal.getName();

        boolean isNewUser = userPrincipal.isNewUser();

        System.out.println("🔥 로그인 사용자 ID = " + userId);
        System.out.println("🔥 신규 사용자 = " + isNewUser);

        // Access Token + Refresh Token 생성
        TokenPair tokens = tokenService.createTokenPair(userId);

        System.out.println("🔥 Access Token 생성됨");
        System.out.println("🔥 Refresh Token 생성됨");

        // Refresh Token → HttpOnly Cookie
        ResponseCookie refreshCookie = ResponseCookie.from(
                        REFRESH_COOKIE,
                        tokens.refreshToken()
                )
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(properties.refreshTokenTtl())
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        // React Callback으로 Access Token 전달
        String redirectUrl = UriComponentsBuilder
                .fromUriString(properties.frontendCallbackUrl())
                .queryParam("token", tokens.accessToken())
                .queryParam("isNewUser", isNewUser)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}