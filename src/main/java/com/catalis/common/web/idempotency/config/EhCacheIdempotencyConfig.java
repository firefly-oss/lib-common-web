package com.catalis.common.web.idempotency.config;

import com.catalis.common.web.idempotency.cache.EhCacheIdempotencyCache;
import com.catalis.common.web.idempotency.cache.IdempotencyCache;
import com.catalis.common.web.idempotency.model.CachedResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Auto-configuration for EhCache-based idempotency cache.
 * This configuration is active when idempotency.cache.ehcache.enabled=true
 * and the required EhCache function beans are available.
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(name = "idempotency.cache.ehcache.enabled", havingValue = "true")
@ConditionalOnBean(name = {"ehcacheIdempotencyGetFunction", "ehcacheIdempotencyPutFunction"})
public class EhCacheIdempotencyConfig {

    private final IdempotencyProperties properties;

    /**
     * Creates a new EhCacheIdempotencyConfig with the specified properties.
     * 
     * @param properties the idempotency configuration properties
     */
    public EhCacheIdempotencyConfig(IdempotencyProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates an IdempotencyCache implementation using EhCache storage.
     * This method expects the application to provide EhCache-specific function beans:
     * - ehcacheIdempotencyGetFunction: Function&lt;String, Mono&lt;CachedResponse&gt;&gt;
     * - ehcacheIdempotencyPutFunction: BiFunction&lt;String, CachedResponse, Mono&lt;Void&gt;&gt;
     * 
     * @param getFunction function to get a value from EhCache
     * @param putFunction function to put a value in EhCache with TTL
     * @return EhCacheIdempotencyCache instance
     */
    @Bean
    public IdempotencyCache idempotencyCacheImpl(
            @Qualifier("ehcacheIdempotencyGetFunction") Function<String, Mono<CachedResponse>> getFunction,
            @Qualifier("ehcacheIdempotencyPutFunction") BiFunction<String, CachedResponse, Mono<Void>> putFunction) {
        
        return new EhCacheIdempotencyCache(getFunction, putFunction);
    }
}