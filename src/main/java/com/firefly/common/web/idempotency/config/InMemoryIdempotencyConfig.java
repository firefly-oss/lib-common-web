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


package com.firefly.common.web.idempotency.config;

import com.firefly.common.web.idempotency.cache.IdempotencyCache;
import com.firefly.common.web.idempotency.cache.InMemoryIdempotencyCache;
import com.firefly.common.web.idempotency.model.CachedResponse;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration for in-memory idempotency cache using Caffeine.
 * This configuration is the default and also serves as a fallback when a Redis-based
 * IdempotencyCache is not available (e.g., redis enabled but no connection factory).
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnMissingBean(IdempotencyCache.class)
public class InMemoryIdempotencyConfig {

    public static final String IDEMPOTENCY_CACHE_NAME = "idempotencyCache";

    private final IdempotencyProperties properties;

    /**
     * Creates a new InMemoryIdempotencyConfig with the specified properties.
     * 
     * @param properties the idempotency configuration properties
     */
    public InMemoryIdempotencyConfig(IdempotencyProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a CaffeineCacheManager for storing idempotent responses.
     * 
     * @return CacheManager instance configured with TTL and maximum size
     */
    @Bean
    public CacheManager idempotencyCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCache().getTtlHours(), TimeUnit.HOURS)
                .maximumSize(properties.getCache().getMaxEntries());

        cacheManager.setCaffeine(caffeine);
        cacheManager.setCacheNames(Collections.singleton(IDEMPOTENCY_CACHE_NAME));

        return cacheManager;
    }

    /**
     * Creates a Cache bean for the idempotency cache.
     * 
     * @param idempotencyCacheManager the cache manager
     * @return Cache instance for idempotency
     */
    @Bean
    public Cache idempotencyCache(CacheManager idempotencyCacheManager) {
        return idempotencyCacheManager.getCache(IDEMPOTENCY_CACHE_NAME);
    }

    /**
     * Creates an IdempotencyCache implementation using in-memory storage.
     * 
     * @param cache the Spring Cache to use for storage
     * @return InMemoryIdempotencyCache instance
     */
    @Bean
    @Primary
    public IdempotencyCache idempotencyCacheImpl(Cache cache) {
        return new InMemoryIdempotencyCache(cache);
    }
}
