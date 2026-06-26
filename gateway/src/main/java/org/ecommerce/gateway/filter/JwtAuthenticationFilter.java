package org.ecommerce.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.ecommerce.gateway.constant.OpenEndpoint;
import org.ecommerce.gateway.constant.SecurityConstants;
import org.ecommerce.gateway.utils.JwtUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {
    private final JwtUtils jwtUtils;

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (OpenEndpoint.openApi.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(SecurityConstants.AUTHORIZATION_HEADER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String token = authHeader.replaceFirst(SecurityConstants.AUTHORIZATION_HEADER_PREFIX, "");
        if (!jwtUtils.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username = jwtUtils.extractUsername(token);
        exchange.getRequest().mutate().header(SecurityConstants.AUTHORIZATION_HEADER_PREFIX, token)
                .header("X-Forwarded-user-name", username).build();
        return chain.filter(exchange);
    }
}
