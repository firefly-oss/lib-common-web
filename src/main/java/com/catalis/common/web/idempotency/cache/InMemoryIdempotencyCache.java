package com.catalis.common.web.idempotency.cache;

import com.catalis.common.web.idempotency.model.CachedResponse;
import org.springframework.cache.Cache;
import reactor.core.publisher.Mono;

/**
 * In-memory implementation of the IdempotencyCache interface using Spring's Cache abstraction.
 */
public class InMemoryIdempotencyCache implements IdempotencyCache {
    
    private final Cache cache;
    
    /**
     * Creates a new InMemoryIdempotencyCache with the specified Spring Cache.
     *
     * @param cache the Spring Cache to use for storage
     */
    public InMemoryIdempotencyCache(Cache cache) {
        this.cache = cache;
    }
    
    @Override
    public Mono<CachedResponse> get(String key) {
        return Mono.fromSupplier(() -> {
            CachedResponse response = cache.get(key, CachedResponse.class);
            return response;
        }).filter(response -> response != null);
    }
    
    @Override
    public Mono<Void> put(String key, CachedResponse response) {
        return Mono.fromRunnable(() -> cache.put(key, response));
    }
}