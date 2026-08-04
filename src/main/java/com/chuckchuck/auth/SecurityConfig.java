package com.chuckchuck.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.chuckchuck.auth.jwt.JwtTokenService;
import com.chuckchuck.auth.oauth.CustomOAuth2UserService;
import com.chuckchuck.auth.oauth.OAuth2LoginSuccessHandler;

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
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error", "/oauth2/**", "/login/**", "/api/auth/refresh")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler(successHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.decoder(tokenService.accessTokenDecoder()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                );
        return http.build();
    }
}
