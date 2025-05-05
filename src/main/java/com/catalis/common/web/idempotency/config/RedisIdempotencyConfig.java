package com.catalis.common.web.idempotency.config;

import com.catalis.common.web.idempotency.cache.IdempotencyCache;
import com.catalis.common.web.idempotency.cache.RedisIdempotencyCache;
import com.catalis.common.web.idempotency.model.CachedResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Auto-configuration for Redis-based idempotency cache.
 * This configuration is active when idempotency.cache.redis.enabled=true
 * and a ReactiveRedisConnectionFactory bean is available.
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(name = "idempotency.cache.redis.enabled", havingValue = "true")
@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
public class RedisIdempotencyConfig {

    private final IdempotencyProperties properties;

    /**
     * Creates a new RedisIdempotencyConfig with the specified properties.
     * 
     * @param properties the idempotency configuration properties
     */
    public RedisIdempotencyConfig(IdempotencyProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a ReactiveRedisTemplate for storing idempotent responses.
     * 
     * @param connectionFactory the Redis connection factory
     * @return ReactiveRedisTemplate instance configured for CachedResponse objects
     */
    @Bean
    public ReactiveRedisTemplate<String, CachedResponse> idempotencyRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<CachedResponse> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(CachedResponse.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, CachedResponse> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, CachedResponse> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }

    /**
     * Creates a ReactiveValueOperations bean for the idempotency cache.
     * This provides a convenient way to interact with Redis values.
     *
     * @param template the ReactiveRedisTemplate for CachedResponse objects
     * @return ReactiveValueOperations for CachedResponse objects
     */
    @Bean
    public ReactiveValueOperations<String, CachedResponse> idempotencyOps(
            ReactiveRedisTemplate<String, CachedResponse> template) {
        return template.opsForValue();
    }

    /**
     * Returns the TTL duration for cached responses.
     *
     * @return Duration representing the TTL
     */
    @Bean
    public Duration idempotencyTtl() {
        return Duration.ofHours(properties.getTtlHours());
    }

    /**
     * Creates an IdempotencyCache implementation using Redis storage.
     * 
     * @param redisOps the ReactiveValueOperations for Redis
     * @param ttl the TTL duration for cache entries
     * @return RedisIdempotencyCache instance
     */
    @Bean
    public IdempotencyCache idempotencyCacheImpl(
            ReactiveValueOperations<String, CachedResponse> redisOps,
            Duration ttl) {
        return new RedisIdempotencyCache(
                key -> redisOps.get(key),
                (key, response) -> redisOps.set(key, response, ttl)
        );
    }
}
