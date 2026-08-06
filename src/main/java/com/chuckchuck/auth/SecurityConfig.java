package com.chuckchuck.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint; // 💡 임포트 추가
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.oauth.CustomOAuth2UserService;
import com.chuckchuck.auth.oauth.OAuth2LoginSuccessHandler;

import java.util.List;
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenService tokenService,
            CustomOAuth2UserService oauth2UserService,
            OAuth2LoginSuccessHandler successHandler,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {

        AuthenticationEntryPoint debugEntryPoint = (request, response, authException) -> {
            System.out.println("🚨 [최종 시큐리티 차단 원인 발견]: " + authException.getMessage());
            authException.printStackTrace();
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // 💡 [수정] /api/oauth2/** 경로를 허용 목록에 명확히 추가합니다.
                        .requestMatchers("/", "/error", "/oauth2/**", "/api/oauth2/**", "/login/**", "/api/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        // 💡 [추가] 리액트가 호출하는 베이스 경로 "/api/oauth2/authorization"과 스프링 내부 엔드포인트를 매핑합니다.
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/api/oauth2/authorization")
                        )
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oauth2UserService))
                        .successHandler(successHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(tokenService.accessTokenDecoder()))
                        .authenticationEntryPoint(debugEntryPoint)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(debugEntryPoint)
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
