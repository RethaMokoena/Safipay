package com.safipay.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Logs every request through the gateway with method, path, status, and latency.
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = Instant.now().toEpochMilli();
        ServerHttpRequest req = exchange.getRequest();
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);

        // Attach request ID for tracing
        ServerHttpRequest mutatedReq = req.mutate()
                .header("X-Request-Id", requestId)
                .build();

        log.info("[{}] --> {} {}", requestId, req.getMethod(), req.getPath());

        return chain.filter(exchange.mutate().request(mutatedReq).build())
                .doFinally(signal -> {
                    long latency = Instant.now().toEpochMilli() - start;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[{}] <-- {} {} {}ms", requestId, status, req.getPath(), latency);
                });
    }

    @Override
    public int getOrder() { return -20; } // Run first
}
