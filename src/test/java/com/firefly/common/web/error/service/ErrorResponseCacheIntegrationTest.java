package com.firefly.common.web.error.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.common.cache.adapter.caffeine.CaffeineCacheAdapter;
import com.firefly.common.cache.config.CaffeineCacheConfig;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.error.config.ErrorHandlingProperties;
import com.firefly.common.web.error.converter.ExceptionConverterService;
import com.firefly.common.web.error.exceptions.ResourceNotFoundException;
import com.firefly.common.web.error.exceptions.ValidationException;
import com.firefly.common.web.error.handler.GlobalExceptionHandler;
import com.firefly.common.web.error.models.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ErrorResponseCache with GlobalExceptionHandler.
 */
class ErrorResponseCacheIntegrationTest {

    private GlobalExceptionHandler exceptionHandler;
    private ErrorResponseCache errorResponseCache;
    private FireflyCacheManager cacheManager;
    private ErrorHandlingProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Create real cache manager with Caffeine
        CaffeineCacheConfig cacheConfig = CaffeineCacheConfig.builder()
                .maxSize(1000L)
                .expireAfterWrite(Duration.ofMinutes(1))
                .recordStats(true)
                .build();

        CaffeineCacheAdapter cacheAdapter = new CaffeineCacheAdapter("default", cacheConfig);
        cacheManager = new FireflyCacheManager(Collections.singletonList(cacheAdapter), "caffeine");

        // Configure error handling properties
        properties = new ErrorHandlingProperties();
        properties.setEnableErrorCaching(true);
        properties.setErrorCacheTtlSeconds(60);
        properties.setErrorCacheMaxSize(1000);
        properties.setIncludeStackTrace(false);
        properties.setIncludeDebugInfo(false);

        // Create error response cache
        errorResponseCache = new ErrorResponseCache(cacheManager, properties);

        // Create exception handler with cache
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        ExceptionConverterService converterService = new ExceptionConverterService(Collections.emptyList());
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        ErrorResponseNegotiator responseNegotiator = new ErrorResponseNegotiator(objectMapper);

        exceptionHandler = new GlobalExceptionHandler(
                converterService,
                Optional.empty(),
                properties,
                Optional.empty(),
                Optional.empty(),
                environment,
                objectMapper,
                responseNegotiator,
                Optional.of(errorResponseCache)
        );
    }

    @Test
    void errorCache_FirstRequest_CachesMissAndStoresResponse() {
        // Given
        ResourceNotFoundException exception = ResourceNotFoundException.forResource("User", "123");
        ServerWebExchange exchange = createExchange("/api/users/123");

        // When - First request (cache miss)
        Mono<Void> result = exceptionHandler.handle(exchange, exception);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Verify cache statistics
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.missCount()).isGreaterThan(0);
    }

    @Test
    void errorCache_SubsequentRequests_ReturnsCachedResponse() throws Exception {
        // Given
        ResourceNotFoundException exception = ResourceNotFoundException.forResource("User", "123");
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/123");

        // When - First request (cache miss)
        exceptionHandler.handle(exchange1, exception).block();

        // Get initial stats
        ErrorResponseCache.CacheStats statsAfterFirst = errorResponseCache.getStats();
        long initialMisses = statsAfterFirst.missCount();

        // When - Second request (should be cache hit)
        exceptionHandler.handle(exchange2, exception).block();

        // Then
        ErrorResponseCache.CacheStats statsAfterSecond = errorResponseCache.getStats();
        assertThat(statsAfterSecond.hitCount()).isGreaterThan(0);
        assertThat(statsAfterSecond.missCount()).isEqualTo(initialMisses); // No new misses

        // Both responses should be identical
        assertThat(exchange1.getResponse().getStatusCode()).isEqualTo(exchange2.getResponse().getStatusCode());
    }

    @Test
    void errorCache_DifferentErrors_CachedSeparately() {
        // Given
        ResourceNotFoundException exception1 = ResourceNotFoundException.forResource("User", "123");
        ResourceNotFoundException exception2 = ResourceNotFoundException.forResource("User", "456");
        
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/456");

        // When
        exceptionHandler.handle(exchange1, exception1).block();
        exceptionHandler.handle(exchange2, exception2).block();

        // Then - Both should be cache misses (different paths)
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.missCount()).isEqualTo(2);
    }

    @Test
    void errorCache_DifferentExceptionTypes_CachedSeparately() {
        // Given
        ResourceNotFoundException notFoundException = ResourceNotFoundException.forResource("User", "123");
        ValidationException validationException = new ValidationException("Validation failed");
        
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/123");

        // When
        exceptionHandler.handle(exchange1, notFoundException).block();
        exceptionHandler.handle(exchange2, validationException).block();

        // Then - Both should be cache misses (different error codes)
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.missCount()).isEqualTo(2);
    }

    @Test
    void errorCache_Invalidate_RemovesCachedEntry() {
        // Given
        ResourceNotFoundException exception = ResourceNotFoundException.forResource("User", "123");
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/123");

        // When - First request (cache miss)
        exceptionHandler.handle(exchange1, exception).block();

        // Invalidate the cache
        errorResponseCache.invalidate("RESOURCE_NOT_FOUND", 404, "/api/users/123").block();

        // Second request (should be cache miss again)
        exceptionHandler.handle(exchange2, exception).block();

        // Then
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.missCount()).isEqualTo(2); // Two misses
    }

    @Test
    void errorCache_Clear_RemovesAllEntries() {
        // Given
        ResourceNotFoundException exception1 = ResourceNotFoundException.forResource("User", "123");
        ResourceNotFoundException exception2 = ResourceNotFoundException.forResource("User", "456");
        
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/456");
        ServerWebExchange exchange3 = createExchange("/api/users/123");
        ServerWebExchange exchange4 = createExchange("/api/users/456");

        // When - Cache two different errors
        exceptionHandler.handle(exchange1, exception1).block();
        exceptionHandler.handle(exchange2, exception2).block();

        // Clear the cache
        errorResponseCache.clear().block();

        // Request both again (should be cache misses)
        exceptionHandler.handle(exchange3, exception1).block();
        exceptionHandler.handle(exchange4, exception2).block();

        // Then - Statistics should be reset
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(0);
        assertThat(stats.missCount()).isEqualTo(0);
    }

    @Test
    void errorCache_HitRate_CalculatedCorrectly() {
        // Given
        ResourceNotFoundException exception = ResourceNotFoundException.forResource("User", "123");

        // When - 1 miss + 3 hits = 75% hit rate
        exceptionHandler.handle(createExchange("/api/users/123"), exception).block(); // miss
        exceptionHandler.handle(createExchange("/api/users/123"), exception).block(); // hit
        exceptionHandler.handle(createExchange("/api/users/123"), exception).block(); // hit
        exceptionHandler.handle(createExchange("/api/users/123"), exception).block(); // hit

        // Then
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(3);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isEqualTo(0.75);
    }

    @Test
    void errorCache_DirectCacheOperations_WorkCorrectly() {
        // Given
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .message("User not found")
                .error("Not Found")
                .build();

        // When - Put directly into cache
        errorResponseCache.put(errorResponse).block();

        // Then - Get from cache
        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123"))
                .assertNext(cached -> {
                    assertThat(cached.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(cached.getStatus()).isEqualTo(404);
                    assertThat(cached.getPath()).isEqualTo("/api/users/123");
                })
                .verifyComplete();

        // Verify stats
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.missCount()).isEqualTo(0);
    }

    @Test
    void errorCache_WithCachingDisabled_DoesNotCache() {
        // Given - Create handler without cache
        ExceptionConverterService converterService = new ExceptionConverterService(Collections.emptyList());
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        ErrorResponseNegotiator responseNegotiator = new ErrorResponseNegotiator(objectMapper);

        GlobalExceptionHandler handlerWithoutCache = new GlobalExceptionHandler(
                converterService,
                Optional.empty(),
                properties,
                Optional.empty(),
                Optional.empty(),
                environment,
                objectMapper,
                responseNegotiator,
                Optional.empty()  // No cache
        );

        ResourceNotFoundException exception = ResourceNotFoundException.forResource("User", "123");
        ServerWebExchange exchange1 = createExchange("/api/users/123");
        ServerWebExchange exchange2 = createExchange("/api/users/123");

        // When
        handlerWithoutCache.handle(exchange1, exception).block();
        handlerWithoutCache.handle(exchange2, exception).block();

        // Then - Both requests should work, but no caching
        assertThat(exchange1.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange2.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ServerWebExchange createExchange(String path) {
        MockServerHttpRequest request = MockServerHttpRequest
                .get(path)
                .build();
        return MockServerWebExchange.from(request);
    }
}

