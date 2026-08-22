package org.ecommerce.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ecommerce.utility.commons.constants.SecurityConstants;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.ecommerce.utility.security.config.JWTPropertiesConfig;
import org.ecommerce.utility.security.exception.SecurityErrorCode;
import org.ecommerce.utility.security.filter.JWTFilter;
import org.ecommerce.utility.security.filter.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectMapper objectMapper;
    private final JWTPropertiesConfig jwtProperties;
    // "/actuator/health/**" is open so the Jenkins pipeline can poll it after a
    // deploy without holding a JWT. Only the health endpoint is exposed
    // (management.endpoints.web.exposure.include), so nothing else is reachable.
    private static final String[] WHITE_LIST = {
            "/api/v1/user/**",
            "/api/v1/users/refresh-token",
            "/api/v1/users/verify-email",
            "/api/v1/users/reset-password",
            "/actuator/health",
            "/actuator/health/**",
    };
    private static final String INTERNAL = "/api/v1/internal/**" ;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(handler ->
                        handler.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().fullyAuthenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
            return http.securityMatcher(INTERNAL)
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .addFilterBefore(new InternalApiKeyFilter(jwtProperties.getApiKey(),objectMapper), UsernamePasswordAuthenticationFilter.class)
                    .build();

    }


    static class InternalApiKeyFilter extends OncePerRequestFilter {
        private final String apiKey;
        private final ObjectMapper objectMapper;

        InternalApiKeyFilter(String apiKey, ObjectMapper objectMapper) {
            this.apiKey = apiKey;
            this.objectMapper = objectMapper;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            // STEP 1: extract API key
            String extractedKey = request.getHeader(SecurityConstants.X_INTERNAL_API_KEY);
            // STEP 2:  return if null
            if (extractedKey == null || !MessageDigest.isEqual(extractedKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
                unauthorized(request, response);
                return;
            }
            filterChain.doFilter(request, response);

        }

        private void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String requestURI = request.getRequestURI();
            SecurityErrorCode invalidApiKey = SecurityErrorCode.INVALID_API_KEY;
            ErrorResponse errorResponse = ErrorResponse.builder().status(invalidApiKey.getHttpStatus().value())
                    .errorCode(invalidApiKey.getCode()).message(invalidApiKey.getMessage())
                    .path(requestURI).timestamp(LocalDateTime.now()).build();
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }


}
