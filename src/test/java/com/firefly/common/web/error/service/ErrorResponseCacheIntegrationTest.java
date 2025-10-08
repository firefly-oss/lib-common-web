package com.firefly.common.web.error.service;

import com.firefly.common.cache.config.CacheAutoConfiguration;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.error.config.ErrorHandlingProperties;
import com.firefly.common.web.error.exceptions.ResourceNotFoundException;
import com.firefly.common.web.error.exceptions.ValidationException;
import com.firefly.common.web.error.models.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ErrorResponseCache with real Caffeine cache.
 * This test uses the actual CacheAutoConfiguration from lib-common-cache to ensure
 * end-to-end functionality with real Caffeine caching.
 */
@SpringBootTest(classes = {
        CacheAutoConfiguration.class
})
@TestPropertySource(properties = {
        "firefly.cache.enabled=true",
        "firefly.cache.default-cache-type=CAFFEINE",
        "firefly.error-handling.enable-error-caching=true",
        "firefly.error-handling.error-cache-ttl-seconds=60",
        "firefly.error-handling.error-cache-max-size=1000"
})
class ErrorResponseCacheIntegrationTest {

    @Autowired
    private FireflyCacheManager cacheManager;

    private ErrorResponseCache errorResponseCache;
    private ErrorHandlingProperties properties;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Configure error handling properties
        properties = new ErrorHandlingProperties();
        properties.setEnableErrorCaching(true);
        properties.setErrorCacheTtlSeconds(60);
        properties.setErrorCacheMaxSize(1000);

        // Create error response cache with real cache manager
        errorResponseCache = new ErrorResponseCache(cacheManager, properties);
    }

    @Test
    void errorCache_PutAndGet_WorksCorrectly() {
        // Given
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .message("User not found")
                .error("Not Found")
                .build();

        // When - Put into cache
        errorResponseCache.put(errorResponse).block();

        // Then - Get from cache
        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123"))
                .assertNext(cached -> {
                    assertThat(cached.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(cached.getStatus()).isEqualTo(404);
                    assertThat(cached.getPath()).isEqualTo("/api/users/123");
                    assertThat(cached.getMessage()).isEqualTo("User not found");
                })
                .verifyComplete();

        // Verify stats
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.missCount()).isEqualTo(0);
    }

    @Test
    void errorCache_GetNonExistent_ReturnsCacheMiss() {
        // When - Get non-existent entry
        StepVerifier.create(errorResponseCache.get("NONEXISTENT", 404, "/api/test"))
                .verifyComplete();

        // Then - Verify miss count
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(0);
        assertThat(stats.missCount()).isEqualTo(1);
    }

    @Test
    void errorCache_MultipleEntries_CachedSeparately() {
        // Given
        ErrorResponse error1 = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .message("User not found")
                .build();

        ErrorResponse error2 = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/456")
                .message("User not found")
                .build();

        ErrorResponse error3 = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .status(400)
                .path("/api/users")
                .message("Validation failed")
                .build();

        // When - Cache multiple errors
        errorResponseCache.put(error1).block();
        errorResponseCache.put(error2).block();
        errorResponseCache.put(error3).block();

        // Then - All should be retrievable
        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123"))
                .assertNext(cached -> assertThat(cached.getPath()).isEqualTo("/api/users/123"))
                .verifyComplete();

        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/456"))
                .assertNext(cached -> assertThat(cached.getPath()).isEqualTo("/api/users/456"))
                .verifyComplete();

        StepVerifier.create(errorResponseCache.get("VALIDATION_ERROR", 400, "/api/users"))
                .assertNext(cached -> assertThat(cached.getCode()).isEqualTo("VALIDATION_ERROR"))
                .verifyComplete();

        // Verify stats
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(3);
        assertThat(stats.missCount()).isEqualTo(0);
    }

    @Test
    void errorCache_Invalidate_RemovesSpecificEntry() {
        // Given
        ErrorResponse error1 = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .message("User not found")
                .build();

        ErrorResponse error2 = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/456")
                .message("User not found")
                .build();

        errorResponseCache.put(error1).block();
        errorResponseCache.put(error2).block();

        // When - Invalidate one entry
        errorResponseCache.invalidate("RESOURCE_NOT_FOUND", 404, "/api/users/123").block();

        // Then - First entry should be gone, second should remain
        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123"))
                .verifyComplete(); // miss

        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/456"))
                .assertNext(cached -> assertThat(cached.getPath()).isEqualTo("/api/users/456"))
                .verifyComplete(); // hit
    }

    @Test
    void errorCache_Clear_RemovesAllEntriesAndResetsStats() {
        // Given
        ErrorResponse error1 = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .build();

        ErrorResponse error2 = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .status(400)
                .path("/api/users")
                .build();

        errorResponseCache.put(error1).block();
        errorResponseCache.put(error2).block();

        // Verify entries exist
        errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123").block();
        errorResponseCache.get("VALIDATION_ERROR", 400, "/api/users").block();

        // Verify stats before clear
        ErrorResponseCache.CacheStats statsBeforeClear = errorResponseCache.getStats();
        assertThat(statsBeforeClear.hitCount()).isEqualTo(2);

        // When - Clear cache
        errorResponseCache.clear().block();

        // Then - Stats should be reset immediately after clear
        ErrorResponseCache.CacheStats statsAfterClear = errorResponseCache.getStats();
        assertThat(statsAfterClear.hitCount()).isEqualTo(0);
        assertThat(statsAfterClear.missCount()).isEqualTo(0);

        // And entries should be gone (these will count as new misses)
        StepVerifier.create(errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123"))
                .verifyComplete();

        StepVerifier.create(errorResponseCache.get("VALIDATION_ERROR", 400, "/api/users"))
                .verifyComplete();
    }

    @Test
    void errorCache_HitRate_CalculatedCorrectly() {
        // Given
        ErrorResponse error = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .status(404)
                .path("/api/users/123")
                .build();

        // When - 1 put + 3 gets = 1 miss + 3 hits = 75% hit rate
        errorResponseCache.put(error).block();
        errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123").block(); // hit
        errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123").block(); // hit
        errorResponseCache.get("RESOURCE_NOT_FOUND", 404, "/api/users/123").block(); // hit

        // Then
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(3);
        assertThat(stats.missCount()).isEqualTo(0);
        assertThat(stats.hitRate()).isEqualTo(1.0); // 100% hit rate (no misses)
    }

    @Test
    void errorCache_WithNullValues_HandlesGracefully() {
        // When - Try to cache null
        StepVerifier.create(errorResponseCache.put(null))
                .verifyComplete();

        // When - Try to cache error with null code
        ErrorResponse errorWithNullCode = ErrorResponse.builder()
                .code(null)
                .status(404)
                .path("/api/test")
                .build();

        StepVerifier.create(errorResponseCache.put(errorWithNullCode))
                .verifyComplete();

        // Then - No errors should occur
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(0);
        assertThat(stats.missCount()).isEqualTo(0);
    }

    @Test
    void errorCache_GetStats_ReturnsAccurateMetrics() {
        // Given
        ErrorResponse error = ErrorResponse.builder()
                .code("TEST_ERROR")
                .status(500)
                .path("/api/test")
                .build();

        // When - Perform various operations
        errorResponseCache.put(error).block();
        errorResponseCache.get("TEST_ERROR", 500, "/api/test").block(); // hit
        errorResponseCache.get("TEST_ERROR", 500, "/api/test").block(); // hit
        errorResponseCache.get("NONEXISTENT", 404, "/api/other").block(); // miss

        // Then
        ErrorResponseCache.CacheStats stats = errorResponseCache.getStats();
        assertThat(stats.hitCount()).isEqualTo(2);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isEqualTo(2.0 / 3.0); // 66.67%
    }
}

