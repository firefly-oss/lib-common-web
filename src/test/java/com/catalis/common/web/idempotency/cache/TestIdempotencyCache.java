package com.catalis.common.web.idempotency.cache;

import com.catalis.common.web.idempotency.model.CachedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of IdempotencyCache for testing.
 */
public class TestIdempotencyCache implements IdempotencyCache {

    private static final Logger log = LoggerFactory.getLogger(TestIdempotencyCache.class);
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    /**
     * Puts a value in the cache directly (not through the reactive API).
     * This is useful for setting up test data.
     *
     * @param key the key
     * @param response the response to cache
     */
    public void putSync(String key, CachedResponse response) {
        log.debug("TestIdempotencyCache.putSync({}) => {}", key, response);
        cache.put(key, response);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
    }

    @Override
    public Mono<CachedResponse> get(String key) {
        CachedResponse response = cache.get(key);
        log.debug("TestIdempotencyCache.get({}) => {}", key, (response != null ? "found" : "not found"));
        return response != null ? Mono.just(response) : Mono.empty();
    }

    @Override
    public Mono<Void> put(String key, CachedResponse response) {
        log.debug("TestIdempotencyCache.put({}) => {}", key, response);
        return Mono.fromRunnable(() -> cache.put(key, response));
    }
}
