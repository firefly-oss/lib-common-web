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

package com.firefly.common.web.idempotency.integration;

import com.firefly.common.cache.config.CacheAutoConfiguration;
import com.firefly.common.cache.core.CacheAdapter;
import com.firefly.common.cache.core.CacheType;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.idempotency.cache.IdempotencyCache;
import com.firefly.common.web.idempotency.config.IdempotencyAutoConfiguration;
import com.firefly.common.web.idempotency.config.IdempotencyProperties;
import com.firefly.common.web.idempotency.model.CachedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies lib-common-web idempotency works correctly with lib-common-cache using real Caffeine cache.
 * This test uses the actual CacheAutoConfiguration from lib-common-cache and IdempotencyAutoConfiguration to ensure
 * end-to-end functionality with real Caffeine caching.
 */
@SpringBootTest(classes = {
    CacheAutoConfiguration.class,
    IdempotencyAutoConfiguration.class
})
@TestPropertySource(properties = {
    "firefly.cache.enabled=true",
    "firefly.cache.default-cache-type=CAFFEINE",
    "firefly.cache.caffeine.idempotency.maximum-size=100",
    "firefly.cache.caffeine.idempotency.expire-after-write=PT5M",
    "firefly.cache.caffeine.idempotency.record-stats=true",
    "idempotency.header-name=X-Idempotency-Key",
    "idempotency.cache.cache-name=idempotency",
    "idempotency.cache.ttl-hours=1"
})
class IdempotencyCacheIntegrationTest {

    @Autowired
    private FireflyCacheManager cacheManager;

    @Autowired
    private IdempotencyCache idempotencyCache;

    @Autowired
    private IdempotencyProperties properties;

    private CacheAdapter cache;

    @BeforeEach
    void setUp() {
        // Get cache adapter directly
        cache = cacheManager.getCache("idempotency");
        assertThat(cache).isNotNull();

        // Clear cache before each test
        cache.clear().block();
    }

    @Test
    void shouldVerifyCaffeineIsConfigured() {
        // Verify we're using the real lib-common-cache implementation with Caffeine
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(FireflyCacheManager.class);
        
        CacheAdapter cache = cacheManager.getCache("idempotency");
        assertThat(cache).isNotNull();
        assertThat(cache.getCacheName()).isEqualTo("idempotency");
        assertThat(cache.getCacheType()).isEqualTo(CacheType.CAFFEINE);
        assertThat(cache.isAvailable()).isTrue();
    }

    @Test
    void shouldVerifyIdempotencyCacheIsConfigured() {
        // Verify idempotency cache is properly configured
        assertThat(idempotencyCache).isNotNull();
        assertThat(properties).isNotNull();
        assertThat(properties.getHeaderName()).isEqualTo("X-Idempotency-Key");
        assertThat(properties.getCache().getCacheName()).isEqualTo("idempotency");
        assertThat(properties.getCache().getTtlHours()).isEqualTo(1);
    }

    @Test
    void shouldCacheResponseWithRealCaffeine() {
        // Given
        String key = "test-key-1";
        CachedResponse response = new CachedResponse(200, "test body".getBytes(), MediaType.APPLICATION_JSON);

        // When - Put response in cache
        StepVerifier.create(idempotencyCache.put(key, response))
            .verifyComplete();

        // Then - Get response from cache
        StepVerifier.create(idempotencyCache.get(key))
            .assertNext(cachedResponse -> {
                assertThat(cachedResponse).isNotNull();
                assertThat(cachedResponse.getStatus()).isEqualTo(200);
                assertThat(cachedResponse.getBody()).isEqualTo("test body".getBytes());
                assertThat(cachedResponse.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        // Given
        String key = "non-existent-key";

        // When - Get non-existent key
        StepVerifier.create(idempotencyCache.get(key))
            .verifyComplete();
    }

    @Test
    void shouldCacheDifferentResponsesSeparately() {
        // Given
        String key1 = "key-1";
        String key2 = "key-2";
        CachedResponse response1 = new CachedResponse(200, "response 1".getBytes(), MediaType.APPLICATION_JSON);
        CachedResponse response2 = new CachedResponse(201, "response 2".getBytes(), MediaType.TEXT_PLAIN);

        // When - Cache two different responses
        idempotencyCache.put(key1, response1).block();
        idempotencyCache.put(key2, response2).block();

        // Then - Both responses should be cached separately
        StepVerifier.create(idempotencyCache.get(key1))
            .assertNext(cached -> {
                assertThat(cached.getStatus()).isEqualTo(200);
                assertThat(cached.getBody()).isEqualTo("response 1".getBytes());
            })
            .verifyComplete();

        StepVerifier.create(idempotencyCache.get(key2))
            .assertNext(cached -> {
                assertThat(cached.getStatus()).isEqualTo(201);
                assertThat(cached.getBody()).isEqualTo("response 2".getBytes());
            })
            .verifyComplete();
    }

    @Test
    void shouldEvictCacheManually() {
        // Given
        String key = "evict-test-key";
        CachedResponse response = new CachedResponse(200, "test".getBytes(), MediaType.APPLICATION_JSON);

        // When - Cache and then evict
        idempotencyCache.put(key, response).block();
        
        // Verify it's cached
        StepVerifier.create(idempotencyCache.get(key))
            .assertNext(cached -> assertThat(cached).isNotNull())
            .verifyComplete();

        // Evict using CacheAdapter directly
        StepVerifier.create(cache.evict(key))
            .expectNext(true)
            .verifyComplete();

        // Then - Should not be in cache anymore
        StepVerifier.create(idempotencyCache.get(key))
            .verifyComplete();
    }

    @Test
    void shouldClearAllCacheEntries() {
        // Given
        String key1 = "clear-key-1";
        String key2 = "clear-key-2";
        CachedResponse response1 = new CachedResponse(200, "test1".getBytes(), MediaType.APPLICATION_JSON);
        CachedResponse response2 = new CachedResponse(200, "test2".getBytes(), MediaType.APPLICATION_JSON);

        // When - Cache multiple entries
        idempotencyCache.put(key1, response1).block();
        idempotencyCache.put(key2, response2).block();

        // Clear all cache using CacheAdapter directly
        StepVerifier.create(cache.clear())
            .verifyComplete();

        // Then - All entries should be cleared
        StepVerifier.create(idempotencyCache.get(key1))
            .verifyComplete();

        StepVerifier.create(idempotencyCache.get(key2))
            .verifyComplete();
    }

    @Test
    void shouldVerifyRealCaffeineOperations() {
        // Given
        String key = "direct-cache-key";
        CachedResponse value = new CachedResponse(200, "direct test".getBytes(), MediaType.APPLICATION_JSON);
        CacheAdapter cache = cacheManager.getCache("idempotency");

        // When - Put value directly in Caffeine cache
        StepVerifier.create(cache.put(key, value))
            .verifyComplete();

        // Then - Get value from Caffeine cache
        StepVerifier.create(cache.<String, CachedResponse>get(key, CachedResponse.class))
            .assertNext(result -> {
                assertThat(result).isPresent();
                assertThat(result.get().getStatus()).isEqualTo(200);
                assertThat(result.get().getBody()).isEqualTo("direct test".getBytes());
            })
            .verifyComplete();

        // When - Evict value
        StepVerifier.create(cache.evict(key))
            .expectNext(true)
            .verifyComplete();

        // Then - Verify evicted from Caffeine
        StepVerifier.create(cache.<String, CachedResponse>get(key, CachedResponse.class))
            .assertNext(result -> assertThat(result).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldVerifyCaffeineStatistics() {
        // Given
        String key = "stats-key";
        CachedResponse response = new CachedResponse(200, "stats test".getBytes(), MediaType.APPLICATION_JSON);
        CacheAdapter cache = cacheManager.getCache("idempotency");

        // When - Perform cache operations (miss, then hit)
        idempotencyCache.get(key).block();  // Miss
        idempotencyCache.put(key, response).block();
        idempotencyCache.get(key).block();  // Hit

        // Then - Verify Caffeine statistics are available
        StepVerifier.create(cache.getStats())
            .assertNext(stats -> {
                assertThat(stats).isNotNull();
                assertThat(stats.getCacheName()).isEqualTo("idempotency");
                assertThat(stats.getCacheType()).isEqualTo(CacheType.CAFFEINE);
                assertThat(stats.getHitCount()).isGreaterThan(0);
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleComplexCachedResponse() {
        // Given
        String key = "complex-key";
        byte[] largeBody = new byte[1024];
        for (int i = 0; i < largeBody.length; i++) {
            largeBody[i] = (byte) (i % 256);
        }
        CachedResponse response = new CachedResponse(201, largeBody, MediaType.APPLICATION_OCTET_STREAM);

        // When - Cache complex response
        StepVerifier.create(idempotencyCache.put(key, response))
            .verifyComplete();

        // Then - Retrieve and verify
        StepVerifier.create(idempotencyCache.get(key))
            .assertNext(cached -> {
                assertThat(cached.getStatus()).isEqualTo(201);
                assertThat(cached.getBody()).hasSize(1024);
                assertThat(cached.getBody()).isEqualTo(largeBody);
                assertThat(cached.getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
            })
            .verifyComplete();
    }
}

