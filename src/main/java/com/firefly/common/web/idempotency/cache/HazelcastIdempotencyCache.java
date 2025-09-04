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
 * Hazelcast implementation of the IdempotencyCache interface.
 * This class uses function references to avoid direct dependencies on Hazelcast classes,
 * allowing the application to start without Hazelcast when it's not needed.
 * 
 * Hazelcast provides distributed caching capabilities suitable for clustered deployments
 * where multiple application instances need to share the same idempotency cache.
 */
public class HazelcastIdempotencyCache implements IdempotencyCache {
    
    private final Function<String, Mono<CachedResponse>> getFunction;
    private final BiFunction<String, CachedResponse, Mono<Boolean>> setFunction;
    
    /**
     * Creates a new HazelcastIdempotencyCache with the specified functions for Hazelcast operations.
     *
     * @param getFunction function to get a value from Hazelcast IMap
     * @param setFunction function to set a value in Hazelcast IMap with TTL
     */
    public HazelcastIdempotencyCache(
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