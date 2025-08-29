package com.firefly.common.web.idempotency.cache;

import com.firefly.common.web.idempotency.model.CachedResponse;
import reactor.core.publisher.Mono;

/**
 * Interface for idempotency caching operations.
 * This abstraction allows for different cache implementations (in-memory, Redis, etc.)
 * without creating direct dependencies on specific technologies.
 */
public interface IdempotencyCache {
    
    /**
     * Gets a cached response by its key.
     *
     * @param key the idempotency key
     * @return a Mono that emits the cached response if found, or empty if not found
     */
    Mono<CachedResponse> get(String key);
    
    /**
     * Puts a response in the cache with the specified key.
     *
     * @param key the idempotency key
     * @param response the response to cache
     * @return a Mono that completes when the operation is done
     */
    Mono<Void> put(String key, CachedResponse response);
}