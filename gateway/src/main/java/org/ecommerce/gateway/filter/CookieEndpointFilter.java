package org.ecommerce.gateway.filter;

import org.ecommerce.gateway.constant.OpenEndpoint;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Removes the Authorization header from requests to the cookie-authenticated endpoints
 * (refresh-token and logout) before they reach auth-service.
 *
 * Why: frontends usually attach "Authorization: Bearer ..." to every call. At refresh time that
 * access token has normally expired, and the shared JWTFilter in each service rejects an expired
 * Bearer on every path - so refresh would fail exactly when it is needed. These two endpoints are
 * authenticated by the refresh cookie alone, so the header is useless there and is dropped.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@NullMarked
public class CookieEndpointFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean hasAuthorization = exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION);

        if (hasAuthorization && OpenEndpoint.cookieAuthenticated.contains(path)) {
            ServerWebExchange withoutAuthorization = exchange.mutate()
                    .request(request -> request.headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION)))
                    .build();
            return chain.filter(withoutAuthorization);
        }
        return chain.filter(exchange);
    }
}
