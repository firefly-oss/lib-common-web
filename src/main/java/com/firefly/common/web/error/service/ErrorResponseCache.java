package com.firefly.common.web.error.service;

import com.firefly.common.web.error.config.ErrorHandlingProperties;
import com.firefly.common.web.error.models.ErrorResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for caching error responses to reduce load during error storms.
 * <p>
 * This service uses Caffeine cache to store error responses based on a cache key
 * that includes the error code, HTTP status, and request path. This helps prevent
 * overwhelming the system when the same error occurs repeatedly.
 * </p>
 * <p>
 * Features:
 * - Configurable TTL and max size
 * - Automatic cache key generation
 * - Cache statistics tracking
 * - Thread-safe operations
 * </p>
 *
 * @author Firefly Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "firefly.error-handling",
        name = "enable-error-caching",
        havingValue = "true"
)
public class ErrorResponseCache {

    private final Cache<CacheKey, ErrorResponse> cache;
    private final ErrorHandlingProperties properties;

    /**
     * Creates a new ErrorResponseCache with the specified configuration.
     *
     * @param properties the error handling properties
     */
    public ErrorResponseCache(ErrorHandlingProperties properties) {
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getErrorCacheMaxSize())
                .expireAfterWrite(Duration.ofSeconds(properties.getErrorCacheTtlSeconds()))
                .recordStats()
                .build();

        log.info("Error response cache initialized with maxSize={}, ttl={}s",
                properties.getErrorCacheMaxSize(),
                properties.getErrorCacheTtlSeconds());
    }

    /**
     * Gets a cached error response if available.
     *
     * @param errorCode the error code
     * @param status the HTTP status code
     * @param path the request path
     * @return the cached error response, or empty if not found
     */
    public Optional<ErrorResponse> get(String errorCode, int status, String path) {
        CacheKey key = new CacheKey(errorCode, status, path);
        ErrorResponse cached = cache.getIfPresent(key);

        if (cached != null) {
            log.debug("Cache HIT for error: code={}, status={}, path={}", errorCode, status, path);
        } else {
            log.debug("Cache MISS for error: code={}, status={}, path={}", errorCode, status, path);
        }

        return Optional.ofNullable(cached);
    }

    /**
     * Caches an error response.
     *
     * @param errorResponse the error response to cache
     */
    public void put(ErrorResponse errorResponse) {
        if (errorResponse == null || errorResponse.getCode() == null) {
            log.warn("Cannot cache error response: null or missing error code");
            return;
        }

        CacheKey key = new CacheKey(
                errorResponse.getCode(),
                errorResponse.getStatus(),
                errorResponse.getPath()
        );

        cache.put(key, errorResponse);
        log.debug("Cached error response: code={}, status={}, path={}",
                errorResponse.getCode(),
                errorResponse.getStatus(),
                errorResponse.getPath());
    }

    /**
     * Invalidates a specific cache entry.
     *
     * @param errorCode the error code
     * @param status the HTTP status code
     * @param path the request path
     */
    public void invalidate(String errorCode, int status, String path) {
        CacheKey key = new CacheKey(errorCode, status, path);
        cache.invalidate(key);
        log.debug("Invalidated cache entry: code={}, status={}, path={}", errorCode, status, path);
    }

    /**
     * Clears all cached error responses.
     */
    public void clear() {
        cache.invalidateAll();
        log.info("Error response cache cleared");
    }

    /**
     * Gets cache statistics.
     *
     * @return cache statistics
     */
    public CacheStats getStats() {
        var stats = cache.stats();
        return new CacheStats(
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate(),
                stats.evictionCount(),
                cache.estimatedSize()
        );
    }

    /**
     * Cache key for error responses.
     */
    private static class CacheKey {
        private final String errorCode;
        private final int status;
        private final String path;

        public CacheKey(String errorCode, int status, String path) {
            this.errorCode = errorCode;
            this.status = status;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return status == cacheKey.status &&
                    Objects.equals(errorCode, cacheKey.errorCode) &&
                    Objects.equals(path, cacheKey.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, status, path);
        }

        @Override
        public String toString() {
            return "CacheKey{" +
                    "errorCode='" + errorCode + '\'' +
                    ", status=" + status +
                    ", path='" + path + '\'' +
                    '}';
        }
    }

    /**
     * Cache statistics.
     */
    public record CacheStats(
            long hitCount,
            long missCount,
            double hitRate,
            long evictionCount,
            long size
    ) {
        @Override
        public String toString() {
            return String.format(
                    "CacheStats{hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d, size=%d}",
                    hitCount, missCount, hitRate * 100, evictionCount, size
            );
        }
    }
}

