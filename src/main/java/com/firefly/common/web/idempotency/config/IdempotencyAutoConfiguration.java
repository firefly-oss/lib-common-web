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

import com.firefly.common.cache.config.CacheAutoConfiguration;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.idempotency.cache.FireflyCacheIdempotencyAdapter;
import com.firefly.common.web.idempotency.cache.IdempotencyCache;
import com.firefly.common.web.idempotency.filter.IdempotencyWebFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the idempotency module using lib-common-cache.
 * 
 * <p>This configuration automatically sets up idempotency support when lib-common-cache
 * is available on the classpath. It creates the necessary beans to enable request
 * idempotency using the X-Idempotency-Key header.</p>
 * 
 * <p>The cache provider (Caffeine, Redis, etc.) is configured through lib-common-cache
 * properties (firefly.cache.*). This configuration only handles the idempotency-specific
 * setup.</p>
 * 
 * <p>Configuration example:</p>
 * <pre>
 * idempotency:
 *   header-name: X-Idempotency-Key
 *   cache:
 *     cache-name: idempotency
 *     ttl-hours: 24
 * 
 * firefly:
 *   cache:
 *     default-cache-type: CAFFEINE
 *     caffeine:
 *       idempotency:
 *         maximum-size: 10000
 *         expire-after-write: PT24H
 * </pre>
 */
@AutoConfiguration(after = CacheAutoConfiguration.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnBean(FireflyCacheManager.class)
public class IdempotencyAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAutoConfiguration.class);

    /**
     * Creates the IdempotencyCache bean using lib-common-cache.
     * 
     * <p>This adapter wraps the FireflyCacheManager to provide idempotency caching
     * functionality. The actual cache provider (Caffeine, Redis, etc.) is determined
     * by the lib-common-cache configuration.</p>
     *
     * @param cacheManager the Firefly cache manager from lib-common-cache
     * @param properties the idempotency configuration properties
     * @return the idempotency cache implementation
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyCache.class)
    public IdempotencyCache idempotencyCache(
            FireflyCacheManager cacheManager,
            IdempotencyProperties properties) {

        log.info("Configuring idempotency cache using lib-common-cache");
        log.info("Cache type: {}, TTL: {} hours",
                cacheManager.getCacheType(),
                properties.getCache().getTtlHours());

        return new FireflyCacheIdempotencyAdapter(cacheManager, properties);
    }

    /**
     * Creates the IdempotencyWebFilter bean.
     * 
     * <p>This filter intercepts HTTP requests and provides idempotency support
     * for POST, PUT, and PATCH requests that include the X-Idempotency-Key header.</p>
     *
     * @param properties the idempotency configuration properties
     * @param cache the idempotency cache implementation
     * @return the idempotency web filter
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyWebFilter.class)
    public IdempotencyWebFilter idempotencyWebFilter(
            IdempotencyProperties properties,
            IdempotencyCache cache) {
        
        log.info("Configuring idempotency web filter with header: {}", 
                properties.getHeaderName());
        
        return new IdempotencyWebFilter(properties, cache);
    }
}

