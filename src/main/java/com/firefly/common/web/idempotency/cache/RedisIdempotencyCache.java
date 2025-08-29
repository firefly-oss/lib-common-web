package com.firefly.common.web.idempotency.cache;

import com.firefly.common.web.idempotency.model.CachedResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Redis implementation of the IdempotencyCache interface.
 * This class uses function references to avoid direct dependencies on Redis classes,
 * allowing the application to start without Redis when it's not needed.
 */
public class RedisIdempotencyCache implements IdempotencyCache {
    
    private final Function<String, Mono<CachedResponse>> getFunction;
    private final BiFunction<String, CachedResponse, Mono<Boolean>> setFunction;
    
    /**
     * Creates a new RedisIdempotencyCache with the specified functions for Redis operations.
     *
     * @param getFunction function to get a value from Redis
     * @param setFunction function to set a value in Redis with TTL
     */
    public RedisIdempotencyCache(
            Function<String, Mono<CachedResponse>> getFunction,
            BiFunction<String, CachedResponse, Mono<Boolean>> setFunction) {
        this.getFunction = getFunction;
        this.setFunction = setFunction;
    }
    
    @Override
    public Mono<CachedResponse> get(String key) {
        return getFunction.apply(key);
    }
    
    @Override
    public Mono<Void> put(String key, CachedResponse response) {
        return setFunction.apply(key, response).then();
    }
}