package org.ecommerce.utility.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.utility.commons.util.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access to resource: {}  {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .message(exceptionMessage(authException))
                .errorCode("Unauthorized")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);

    }

    private String exceptionMessage(AuthenticationException exception) {

        return switch (exception) {
            case BadCredentialsException ignored -> "Invalid authentication token";
            case InsufficientAuthenticationException ignored -> "Authentication required";
            default -> "Authentication failed: " + exception.getMessage();
        };
    }
}
