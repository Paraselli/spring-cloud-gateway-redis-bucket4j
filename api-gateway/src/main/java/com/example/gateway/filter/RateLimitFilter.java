package com.example.gateway.filter;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyBucket;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimitFilter implements GatewayFilter {

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;

    public RateLimitFilter(
            ProxyManager<String> proxyManager,
            BucketConfiguration bucketConfiguration) {
        this.proxyManager = proxyManager;
        this.bucketConfiguration = bucketConfiguration;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String clientKey = resolveClientKey(exchange);

        ProxyBucket bucket = proxyManager.builder()
                .build(clientKey, bucketConfiguration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {

            exchange.getResponse().getHeaders().add(
                    "X-RateLimit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );

            return chain.filter(exchange);
        }

        long retryAfterSeconds =
                Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("Retry-After", String.valueOf(retryAfterSeconds));
        headers.add("X-RateLimit-Remaining", "0");

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

        byte[] body = (
                "{\"error\":\"Too many requests\"," +
                "\"message\":\"Rate limit exceeded\"," +
                "\"retryAfterSeconds\":" + retryAfterSeconds + "}"
        ).getBytes(StandardCharsets.UTF_8);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String resolveClientKey(ServerWebExchange exchange) {

        String forwardedFor = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {

            return "ip:" + exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
        }

        return "ip:unknown";
    }
}
