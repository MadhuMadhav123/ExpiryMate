package com.expirymate.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CLASS_NAME = RequestLoggingFilter.class.getSimpleName();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String requestId = exchange.getRequest().getId();

        log.info("{} - Incoming request method: {}, path: {}, requestId: {}", CLASS_NAME, method, path, requestId);

        return chain.filter(exchange)
                .doOnSuccess(unused -> {
                    long duration = System.currentTimeMillis() - startTime;

                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("{} - Request completed method: {}, path: {}, status: {}, durationMs: {}, requestId: {}", CLASS_NAME, method, path, status, duration, requestId);
                })
                .doOnError(exception -> {
                    long duration = System.currentTimeMillis() - startTime;

                    log.error("{} - Request failed method: {}, path: {}, durationMs: {}, requestId: {}, reason: {}", CLASS_NAME, method, path, duration, requestId, exception.getMessage(), exception);
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}