package com.safipay.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple token-bucket rate limiter: 100 requests/min per IP.
 * In production, replace with Redis-backed rate limiting.
 */
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = getClientIp(exchange);
        BucketState bucket = buckets.computeIfAbsent(ip, k -> new BucketState());

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Remaining", String.valueOf(bucket.remaining()));
        return chain.filter(exchange);
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) return forwarded.split(",")[0].trim();
        var addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() { return -10; } // Run before auth filter

    static class BucketState {
        private final AtomicInteger tokens = new AtomicInteger(MAX_REQUESTS_PER_MINUTE);
        private volatile long windowStart = Instant.now().getEpochSecond();

        boolean tryConsume() {
            long now = Instant.now().getEpochSecond();
            // Reset window every minute
            if (now - windowStart >= 60) {
                tokens.set(MAX_REQUESTS_PER_MINUTE);
                windowStart = now;
            }
            return tokens.decrementAndGet() >= 0;
        }

        int remaining() {
            return Math.max(0, tokens.get());
        }
    }
}
