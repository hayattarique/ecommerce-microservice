package org.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.gateway.constant.OpenEndpoint;
import org.ecommerce.gateway.constant.SecurityConstants;
import org.ecommerce.gateway.utils.ErrorResponse;
import org.ecommerce.gateway.utils.GatewayErrorCode;
import org.ecommerce.gateway.utils.JwtUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@NullMarked
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String endpoint = request.getURI().getPath();

        // public endpoints — login, register, refresh
        if (OpenEndpoint.openApi.stream().anyMatch(endpoint::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(SecurityConstants.AUTHORIZATION_HEADER_PREFIX)) {
            return unauthorized(exchange, GatewayErrorCode.MISSING_TOKEN);
        }

        String token = authHeader.substring(SecurityConstants.AUTHORIZATION_HEADER_PREFIX.length());

        Claims claims;
        try {
            claims = jwtUtils.extractAllClaims(token);
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, GatewayErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            // bad signature, malformed token, unsupported algorithm, blank token
            return unauthorized(exchange, GatewayErrorCode.INVALID_TOKEN);
        }

        // a refresh token is only valid at /auth/refresh-token, never as a credential
        if (!SecurityConstants.ACCESS_TOKEN.equals(claims.get(SecurityConstants.TOKEN_TYPE, String.class))) {
            return unauthorized(exchange, GatewayErrorCode.INVALID_TOKEN_TYPE);
        }

        // auth-service writes this claim as a number — read it as Long, not String
        Long userAccountId = claims.get(SecurityConstants.USER_ACCOUNT_ID_CLAIM, Long.class);

        ServerWebExchange mutated = exchange.mutate()
                .request(re -> re.header(SecurityConstants.USER_ACCOUNT_ID_HEADER, String.valueOf(userAccountId)))
                .build();

        return chain.filter(mutated);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, GatewayErrorCode errorCode) {

        String path = exchange.getRequest().getURI().getPath();
        log.warn("Gateway rejected {} {} — [{}] {}",
                exchange.getRequest().getMethod(), path, errorCode.code(), errorCode.message());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), errorCode.code(), errorCode.message(), path);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(errorResponse));
        return response.writeWith(Mono.just(buffer));
    }
}
