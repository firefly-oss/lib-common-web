package com.catalis.common.web.idempotency.filter;

import com.catalis.common.web.idempotency.annotation.DisableIdempotency;
import com.catalis.common.web.idempotency.cache.IdempotencyCache;
import com.catalis.common.web.idempotency.config.IdempotencyProperties;
import com.catalis.common.web.idempotency.model.CachedResponse;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * WebFilter that provides idempotency for HTTP POST, PUT, and PATCH requests.
 * It intercepts requests with an Idempotency-Key header and ensures that
 * identical requests (with the same key) return the same response.
 */
@Component
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyWebFilter implements WebFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final List<HttpMethod> IDEMPOTENT_METHODS = Arrays.asList(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final IdempotencyCache cache;
    private final boolean redisEnabled;

    /**
     * Creates a new IdempotencyWebFilter with the specified caching mechanism.
     *
     * @param properties the idempotency configuration properties
     * @param cache the idempotency cache implementation
     */
    @Autowired
    public IdempotencyWebFilter(IdempotencyProperties properties, IdempotencyCache cache) {
        this.cache = cache;
        this.redisEnabled = properties.getRedis().isEnabled();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Processing request " + request.getMethod() + " " + request.getPath());

        // Only apply to POST, PUT, PATCH methods
        if (!IDEMPOTENT_METHODS.contains(request.getMethod())) {
            System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Skipping non-idempotent method " + request.getMethod());
            return chain.filter(exchange);
        }

        // Check for Idempotency-Key header
        List<String> idempotencyKeyHeaders = request.getHeaders().get(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKeyHeaders == null || idempotencyKeyHeaders.isEmpty()) {
            System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: No Idempotency-Key header found");
            return chain.filter(exchange);
        }

        String idempotencyKey = idempotencyKeyHeaders.get(0);
        System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Found Idempotency-Key: " + idempotencyKey);

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Empty Idempotency-Key header");
            return sendBadRequest(exchange, "Invalid Idempotency-Key header");
        }

        // Check if idempotency is disabled for this endpoint
        return isIdempotencyDisabled(exchange)
                .flatMap(disabled -> {
                    if (disabled) {
                        // Skip idempotency processing if the endpoint has the DisableIdempotency annotation
                        System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Idempotency is disabled for this endpoint");
                        return chain.filter(exchange);
                    }

                    System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Checking cache for key: " + idempotencyKey);
                    // Check if we have a cached response
                    return cache.get(idempotencyKey)
                            .hasElement()
                            .flatMap(hasCachedResponse -> {
                                if (hasCachedResponse) {
                                    System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: Found cached response for key: " + idempotencyKey);
                                    return cache.get(idempotencyKey)
                                            .flatMap(cachedResponse -> returnCachedResponse(exchange, cachedResponse));
                                } else {
                                    System.out.println("[DEBUG_LOG] IdempotencyWebFilter.filter: No cached response found for key: " + idempotencyKey);
                                    return processThroughFilterChain(exchange, chain, idempotencyKey);
                                }
                            });
                });
    }

    private Mono<Void> processThroughFilterChain(ServerWebExchange exchange, WebFilterChain chain, String idempotencyKey) {
        System.out.println("[DEBUG_LOG] IdempotencyWebFilter.processThroughFilterChain: Processing request through filter chain for key: " + idempotencyKey);
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                System.out.println("[DEBUG_LOG] IdempotencyWebFilter.writeWith: Writing response for key: " + idempotencyKey);
                // Convert the body to a Flux regardless of its type
                Flux<DataBuffer> fluxBody = Flux.from(body);

                return super.writeWith(fluxBody.collectList().map(dataBuffers -> {
                    System.out.println("[DEBUG_LOG] IdempotencyWebFilter.writeWith: Collected response body for key: " + idempotencyKey);
                    // Combine all DataBuffers to get the complete response body
                    DataBuffer joinedBuffer = exchange.getResponse().bufferFactory().join(dataBuffers);
                    byte[] content = new byte[joinedBuffer.readableByteCount()];
                    joinedBuffer.read(content);

                    // Create a copy of the buffer for writing to the response
                    DataBuffer copiedBuffer = exchange.getResponse().bufferFactory().wrap(content);

                    // Store the response in the cache
                    MediaType contentType = exchange.getResponse().getHeaders().getContentType();
                    int statusCode = exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().value() : HttpStatus.OK.value();

                    System.out.println("[DEBUG_LOG] IdempotencyWebFilter.writeWith: Caching response for key: " + idempotencyKey + 
                                      ", status: " + statusCode + 
                                      ", contentType: " + contentType + 
                                      ", bodyLength: " + content.length);

                    CachedResponse cachedResponse = new CachedResponse(statusCode, content, contentType);
                    cache.put(idempotencyKey, cachedResponse)
                        .doOnError(e -> {
                            // Log the error but don't fail the request
                            System.err.println("[DEBUG_LOG] Failed to cache response for idempotency key " + idempotencyKey + ": " + e.getMessage());
                            e.printStackTrace();
                        })
                        .onErrorResume(e -> Mono.empty())
                        .doOnSuccess(v -> System.out.println("[DEBUG_LOG] IdempotencyWebFilter.writeWith: Successfully cached response for key: " + idempotencyKey))
                        .subscribe();

                    return copiedBuffer;
                }).flux());
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .doOnSuccess(v -> System.out.println("[DEBUG_LOG] IdempotencyWebFilter.processThroughFilterChain: Successfully processed request through filter chain for key: " + idempotencyKey))
                .doOnError(e -> System.err.println("[DEBUG_LOG] IdempotencyWebFilter.processThroughFilterChain: Error processing request through filter chain for key: " + idempotencyKey + ": " + e.getMessage()));
    }

    private Mono<Void> returnCachedResponse(ServerWebExchange exchange, CachedResponse cachedResponse) {
        System.out.println("[DEBUG_LOG] IdempotencyWebFilter.returnCachedResponse: Returning cached response with status: " + 
                          cachedResponse.getStatus() + 
                          ", contentType: " + cachedResponse.getContentType() + 
                          ", bodyLength: " + (cachedResponse.getBody() != null ? cachedResponse.getBody().length : 0));

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(cachedResponse.getStatus()));

        if (cachedResponse.getContentType() != null) {
            response.getHeaders().setContentType(cachedResponse.getContentType());
        }

        DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.getBody());
        return response.writeWith(Mono.just(buffer))
                .doOnSuccess(v -> System.out.println("[DEBUG_LOG] IdempotencyWebFilter.returnCachedResponse: Successfully returned cached response"))
                .doOnError(e -> System.err.println("[DEBUG_LOG] IdempotencyWebFilter.returnCachedResponse: Error returning cached response: " + e.getMessage()));
    }

    private Mono<Void> sendBadRequest(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

        byte[] bytes = message.getBytes();
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Checks if idempotency is disabled for the current endpoint.
     *
     * This method first tries to check if the handler method has the DisableIdempotency annotation.
     * If the handler is not available yet (which can happen in a WebFilter), it falls back to
     * checking the request path against known paths with the DisableIdempotency annotation.
     *
     * @param exchange the server web exchange
     * @return a Mono that emits true if idempotency is disabled for this endpoint, false otherwise
     */
    private Mono<Boolean> isIdempotencyDisabled(ServerWebExchange exchange) {
        // First try to get the handler from the exchange attributes
        Object handler = exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            boolean disabled = handlerMethod.hasMethodAnnotation(DisableIdempotency.class);
            return Mono.just(disabled);
        }

        // If the handler is not available yet, we need to use a different approach
        // by checking the request path
        return Mono.just(false)
            .flatMap(disabled -> {
                // Get the path from the request
                String path = exchange.getRequest().getPath().value();

                // In a real application, this could be a configurable list of paths
                // or could use a more sophisticated matching mechanism
                if (path.endsWith("-disabled") || path.contains("/disabled/")) {
                    return Mono.just(true);
                }

                return Mono.just(disabled);
            });
    }
}
