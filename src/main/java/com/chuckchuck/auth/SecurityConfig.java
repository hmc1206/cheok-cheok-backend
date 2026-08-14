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

        // 💡 [핵심 추가] 최외곽 예외 처리와 토큰 필터 양쪽 모두에서 원인을 찍어줄 공통 디버깅 엔트리 포인트를 만듭니다.
        AuthenticationEntryPoint debugEntryPoint = (request, response, authException) -> {
            System.out.println("🚨 [최종 시큐리티 차단 원인 발견]: " + authException.getMessage());
            authException.printStackTrace(); // 에러 스택트레이스를 인텔리제이에 강제로 출력!
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/oauth2/**", "/login/**","/api/auth/refresh", "/api/v1/routes/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oauth2UserService))
                        .successHandler(successHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(tokenService.accessTokenDecoder()))
                        // 💡 토큰 리소스 서버 엔트리 포인트를 디버깅용으로 교체
                        .authenticationEntryPoint(debugEntryPoint)
                )
                .exceptionHandling(exceptions -> exceptions
                        // 💡 [가장 중요] 로그를 가로채서 숨기던 최외곽 엔트리 포인트까지 디버깅용으로 완벽하게 교체합니다!
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
