package com.chuckchuck.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.chuckchuck.auth.AuthProperties;
import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@Service
public class JwtTokenService {
    private static final String TOKEN_TYPE = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder refreshTokenDecoder;
    private final JwtDecoder accessTokenDecoder;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    @Autowired
    public JwtTokenService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenService(AuthProperties properties, Clock clock) {
        byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.refreshTokenDecoder = decoder(secretKey, REFRESH);
        this.accessTokenDecoder = decoder(secretKey, ACCESS);
        this.accessTokenTtl = properties.accessTokenTtl();
        this.refreshTokenTtl = properties.refreshTokenTtl();
        this.clock = clock;
    }

    public TokenPair createTokenPair(String userId) {
        return new TokenPair(createAccessToken(userId), createToken(userId, REFRESH, refreshTokenTtl));
    }

    public String createAccessToken(String userId) {
        return createToken(userId, ACCESS, accessTokenTtl);
    }

    public String requireRefreshSubject(String token) {
        try {
            Jwt jwt = refreshTokenDecoder.decode(token);

            System.out.println("🔥 Refresh Token 검증 성공");
            System.out.println("🔥 subject = " + jwt.getSubject());
            System.out.println("🔥 expiresAt = " + jwt.getExpiresAt());
            System.out.println("🔥 type = " + jwt.getClaimAsString(TOKEN_TYPE));

            return jwt.getSubject();

        } catch (JwtException | IllegalArgumentException exception) {
            System.out.println("🔥🔥🔥 Refresh Token 검증 실패");
            System.out.println("🔥 예외 = " + exception.getClass().getName());
            System.out.println("🔥 원인 = " + exception.getMessage());

            exception.printStackTrace();

            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    public JwtDecoder accessTokenDecoder() {
        return accessTokenDecoder;
    }

    private String createToken(String userId, String type, Duration ttl) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .claim(TOKEN_TYPE, type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private JwtDecoder decoder(SecretKey secretKey, String requiredType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // 💡 [수정 사항] 생성 시점 시간 오차(Clock Skew)로 인한 401 차단을 막기 위해 기본 검증기에 60초의 유연성을 부여합니다.
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(60));

        OAuth2TokenValidator<Jwt> typeValidator = jwt -> requiredType.equals(jwt.getClaimAsString(TOKEN_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "Unexpected token type",
                null
        ));

        // 💡 시간 검증기(timestampValidator)를 명시적으로 결합해 줍니다.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                typeValidator
        ));
        return decoder;
    }
}
