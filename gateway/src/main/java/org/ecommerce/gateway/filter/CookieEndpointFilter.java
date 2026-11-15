package org.ecommerce.gateway.filter;

import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CookieEndpointFilter implements GlobalFilter {


    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        boolean hasAuthorization = exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION);
        if (hasAuthorization && OpenEndpoint.COOKIE_API_ENDPOINT.contains(path)) {

            exchange = exchange.mutate().request(req -> req.headers(head -> head.remove(HttpHeaders.AUTHORIZATION))).build();
            return chain.filter(exchange);
        }


        return chain.filter(exchange);
    }
}
