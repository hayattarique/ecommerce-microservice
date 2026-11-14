package com.ecommerce.product.config;

import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.security.filter.JWTFilter;
import org.ecommerce.utility.security.filter.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    // Health is open so the pipeline can poll it; the OpenAPI docs are open so the
    // gateway's Swagger UI can load them (the gateway already lets them through).
    private static final String[] WHITE_LIST = {
            "/actuator/health", "/actuator/health/**",
            "/v3/api-docs", "/v3/api-docs/**",
            // Spring forwards errors to /error internally; blocking it turns every 400/500 into a fake 401
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(handler ->
                        handler.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().fullyAuthenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
