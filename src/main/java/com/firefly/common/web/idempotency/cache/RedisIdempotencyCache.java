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