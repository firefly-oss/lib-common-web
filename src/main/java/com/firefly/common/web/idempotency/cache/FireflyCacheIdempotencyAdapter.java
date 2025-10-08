/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.web.idempotency.cache;

import com.firefly.common.cache.core.CacheAdapter;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.idempotency.config.IdempotencyProperties;
import com.firefly.common.web.idempotency.model.CachedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Adapter that bridges the IdempotencyCache interface with the Firefly Common Cache library.
 * This adapter wraps the FireflyCacheManager to provide idempotency caching functionality
 * using the unified caching abstraction from lib-common-cache.
 * 
 * <p>This implementation delegates all cache operations to the underlying FireflyCacheManager,
 * which can be configured to use different cache providers (Caffeine, Redis, etc.) through
 * the lib-common-cache configuration.</p>
 */
public class FireflyCacheIdempotencyAdapter implements IdempotencyCache {

    private static final Logger log = LoggerFactory.getLogger(FireflyCacheIdempotencyAdapter.class);

    private final FireflyCacheManager cacheManager;
    private final String cacheName;
    private final Duration ttl;

    /**
     * Creates a new FireflyCacheIdempotencyAdapter.
     *
     * @param cacheManager the Firefly cache manager
     * @param properties the idempotency configuration properties
     */
    public FireflyCacheIdempotencyAdapter(FireflyCacheManager cacheManager, IdempotencyProperties properties) {
        this.cacheManager = cacheManager;
        this.cacheName = properties.getCache().getCacheName();
        this.ttl = Duration.ofHours(properties.getCache().getTtlHours());
        
        log.info("Initialized FireflyCacheIdempotencyAdapter with cache: {}, TTL: {} hours", 
                cacheName, properties.getCache().getTtlHours());
    }

    @Override
    public Mono<CachedResponse> get(String key) {
        log.debug("Getting cached response for key: {}", key);
        
        CacheAdapter cache = getCache();
        return cache.<String, CachedResponse>get(key, CachedResponse.class)
                .flatMap(optional -> {
                    if (optional.isPresent()) {
                        log.debug("Cache hit for key: {}", key);
                        return Mono.just(optional.get());
                    } else {
                        log.debug("Cache miss for key: {}", key);
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error getting cached response for key {}: {}", key, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> put(String key, CachedResponse response) {
        log.debug("Putting cached response for key: {}", key);
        
        CacheAdapter cache = getCache();
        return cache.put(key, response, ttl)
                .doOnSuccess(v -> log.debug("Successfully cached response for key: {}", key))
                .onErrorResume(e -> {
                    log.error("Error caching response for key {}: {}", key, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * Gets the cache adapter for the configured cache name.
     * If a specific cache is configured, it will use that cache.
     * Otherwise, it will use the default cache from the cache manager.
     *
     * @return the cache adapter
     * @throws IllegalStateException if no cache is available
     */
    private CacheAdapter getCache() {
        if (cacheName != null && !cacheName.isEmpty() && cacheManager.hasCache(cacheName)) {
            CacheAdapter cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                return cache;
            }
        }
        
        // Fall back to default cache
        return cacheManager.getDefaultCache();
    }
}

