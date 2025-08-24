package com.catalis.common.web.idempotency.config;

import com.catalis.common.web.idempotency.cache.HazelcastIdempotencyCache;
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

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Auto-configuration for Hazelcast-based idempotency cache.
 * This configuration is active when idempotency.cache.hazelcast.enabled=true
 * and the required Hazelcast function beans are available.
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(name = "idempotency.cache.hazelcast.enabled", havingValue = "true")
@ConditionalOnBean(name = {"hazelcastIdempotencyGetFunction", "hazelcastIdempotencySetFunction"})
public class HazelcastIdempotencyConfig {

    private final IdempotencyProperties properties;

    /**
     * Creates a new HazelcastIdempotencyConfig with the specified properties.
     * 
     * @param properties the idempotency configuration properties
     */
    public HazelcastIdempotencyConfig(IdempotencyProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates an IdempotencyCache implementation using Hazelcast storage.
     * This method expects the application to provide Hazelcast-specific function beans:
     * - hazelcastIdempotencyGetFunction: Function&lt;String, Mono&lt;CachedResponse&gt;&gt;
     * - hazelcastIdempotencySetFunction: BiFunction&lt;String, CachedResponse, Mono&lt;Boolean&gt;&gt;
     * 
     * @param getFunction function to get a value from Hazelcast IMap
     * @param setFunction function to set a value in Hazelcast IMap with TTL
     * @return HazelcastIdempotencyCache instance
     */
    @Bean
    public IdempotencyCache idempotencyCacheImpl(
            @Qualifier("hazelcastIdempotencyGetFunction") Function<String, Mono<CachedResponse>> getFunction,
            @Qualifier("hazelcastIdempotencySetFunction") BiFunction<String, CachedResponse, Mono<Boolean>> setFunction) {
        
        return new HazelcastIdempotencyCache(getFunction, setFunction);
    }
}