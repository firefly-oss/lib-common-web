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

import com.firefly.common.web.idempotency.model.CachedResponse;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * EhCache implementation of the IdempotencyCache interface.
 * This class uses function references to avoid direct dependencies on EhCache classes,
 * allowing the application to start without EhCache when it's not needed.
 * 
 * EhCache provides local caching with optional disk persistence, suitable for 
 * single-instance deployments that require cache persistence across application restarts.
 * It offers better performance than distributed caches for local scenarios while 
 * providing durability features that in-memory caches lack.
 */
public class EhCacheIdempotencyCache implements IdempotencyCache {
    
    private final Function<String, Mono<CachedResponse>> getFunction;
    private final BiFunction<String, CachedResponse, Mono<Void>> putFunction;
    
    /**
     * Creates a new EhCacheIdempotencyCache with the specified functions for EhCache operations.
     *
     * @param getFunction function to get a value from EhCache
     * @param putFunction function to put a value in EhCache with TTL
     */
    public EhCacheIdempotencyCache(
            Function<String, Mono<CachedResponse>> getFunction,
            BiFunction<String, CachedResponse, Mono<Void>> putFunction) {
        this.getFunction = getFunction;
        this.putFunction = putFunction;
    }
    
    @Override
    public Mono<CachedResponse> get(String key) {
        return getFunction.apply(key);
    }
    
    @Override
    public Mono<Void> put(String key, CachedResponse response) {
        return putFunction.apply(key, response);
    }
}