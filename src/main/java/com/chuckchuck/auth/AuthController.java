package com.chuckchuck.auth;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.oauth.OAuth2LoginSuccessHandler;
import com.chuckchuck.auth.user.AppUserRepository;
import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtTokenService tokenService;
    private final AppUserRepository userRepository;

    public AuthController(JwtTokenService tokenService, AppUserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(
            @CookieValue(name = OAuth2LoginSuccessHandler.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        String userId = tokenService.requireRefreshSubject(refreshToken);
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return new AccessTokenResponse(tokenService.createAccessToken(userId));
    }
}
