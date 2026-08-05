package com.chuckchuck.auth.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

        // 💡 [수정 사항] 구글 로그인(OidcUser)과 커스텀 유저 타입을 모두 지원하도록 안전하게 형변환 처리
        Object principalObj = authentication.getPrincipal();
        String username;
        boolean isNewUser = false;

        if (principalObj instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) principalObj;
            username = oidcUser.getName(); // 토큰 발급에 사용할 고유 식별자 (sub 값)
            // OidcUser 자체는 커스텀 필드(isNewUser)가 없으므로 기본적으로 false 세팅
            // 만약 DB 조회 로직이 필요하다면 여기에 추가하실 수 있습니다.
        } else if (principalObj instanceof OAuthUserPrincipal) {
            OAuthUserPrincipal customPrincipal = (OAuthUserPrincipal) principalObj;
            username = customPrincipal.getName();
            isNewUser = customPrincipal.isNewUser();
        } else {
            throw new ServletException("지원하지 않는 인증 Principal 타입입니다: " + principalObj.getClass().getName());
        }

        // 💡 기존 토큰 생성 로직 그대로 유지 (추출한 username 사용)
        TokenPair tokens = tokenService.createTokenPair(username);

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path("/api/auth/refresh")
                .maxAge(properties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 💡 기존 리다이렉트 로직 그대로 유지
        String redirectUrl = UriComponentsBuilder.fromUriString(properties.frontendCallbackUrl())
                .queryParam("token", tokens.accessToken())
                .queryParam("isNewUser", isNewUser)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
