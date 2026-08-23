package org.ecommerce.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NullMarked;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Log4j2
@RequiredArgsConstructor
public class TraceIdResponseFilter implements GlobalFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final Tracer tracer;

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() -> {
            Span span = tracer.currentSpan();
            if (span != null) {
                exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, span.context().traceId());
            }
        }));

        return chain.filter(exchange);
    }
}
