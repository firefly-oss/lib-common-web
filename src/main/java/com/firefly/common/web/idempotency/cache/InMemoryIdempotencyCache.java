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