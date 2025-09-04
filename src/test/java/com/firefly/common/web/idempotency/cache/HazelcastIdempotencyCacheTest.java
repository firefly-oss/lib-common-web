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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HazelcastIdempotencyCache.
 * These tests verify that the cache properly delegates to the provided function references.
 */
class HazelcastIdempotencyCacheTest {

    private Function<String, Mono<CachedResponse>> mockGetFunction;
    private BiFunction<String, CachedResponse, Mono<Boolean>> mockSetFunction;
    private HazelcastIdempotencyCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockGetFunction = mock(Function.class);
        mockSetFunction = mock(BiFunction.class);
        cache = new HazelcastIdempotencyCache(mockGetFunction, mockSetFunction);
    }

    @Test
    void shouldDelegateGetToProvidedFunction() {
        // Arrange
        String key = "test-key";
        CachedResponse expectedResponse = new CachedResponse(
                200,
                "test response".getBytes(),
                MediaType.APPLICATION_JSON
        );
        when(mockGetFunction.apply(key)).thenReturn(Mono.just(expectedResponse));

        // Act
        Mono<CachedResponse> result = cache.get(key);

        // Assert
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();
        
        verify(mockGetFunction, times(1)).apply(key);
    }

    @Test
    void shouldDelegateGetToProvidedFunctionWhenEmpty() {
        // Arrange
        String key = "nonexistent-key";
        when(mockGetFunction.apply(key)).thenReturn(Mono.empty());

        // Act
        Mono<CachedResponse> result = cache.get(key);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(mockGetFunction, times(1)).apply(key);
    }

    @Test
    void shouldDelegateGetToProvidedFunctionWhenError() {
        // Arrange
        String key = "error-key";
        RuntimeException expectedException = new RuntimeException("Hazelcast connection error");
        when(mockGetFunction.apply(key)).thenReturn(Mono.error(expectedException));

        // Act
        Mono<CachedResponse> result = cache.get(key);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        
        verify(mockGetFunction, times(1)).apply(key);
    }

    @Test
    void shouldDelegatePutToProvidedFunction() {
        // Arrange
        String key = "test-key";
        CachedResponse response = new CachedResponse(
                201,
                "created response".getBytes(),
                MediaType.APPLICATION_JSON
        );
        when(mockSetFunction.apply(key, response)).thenReturn(Mono.just(true));

        // Act
        Mono<Void> result = cache.put(key, response);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(mockSetFunction, times(1)).apply(key, response);
    }

    @Test
    void shouldDelegatePutToProvidedFunctionWhenSetReturnsError() {
        // Arrange
        String key = "error-key";
        CachedResponse response = new CachedResponse(
                500,
                "error response".getBytes(),
                MediaType.APPLICATION_JSON
        );
        RuntimeException expectedException = new RuntimeException("Hazelcast write error");
        when(mockSetFunction.apply(key, response)).thenReturn(Mono.error(expectedException));

        // Act
        Mono<Void> result = cache.put(key, response);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
        
        verify(mockSetFunction, times(1)).apply(key, response);
    }

    @Test
    void shouldHandleNullKey() {
        // Arrange
        String nullKey = null;
        when(mockGetFunction.apply(nullKey)).thenReturn(Mono.empty());

        // Act
        Mono<CachedResponse> result = cache.get(nullKey);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(mockGetFunction, times(1)).apply(nullKey);
    }

    @Test
    void shouldHandleMultipleConcurrentRequests() {
        // Arrange
        String key1 = "key1";
        String key2 = "key2";
        CachedResponse response1 = new CachedResponse(200, "response1".getBytes(), MediaType.APPLICATION_JSON);
        CachedResponse response2 = new CachedResponse(200, "response2".getBytes(), MediaType.APPLICATION_JSON);
        
        when(mockGetFunction.apply(key1)).thenReturn(Mono.just(response1));
        when(mockGetFunction.apply(key2)).thenReturn(Mono.just(response2));

        // Act
        Mono<CachedResponse> result1 = cache.get(key1);
        Mono<CachedResponse> result2 = cache.get(key2);

        // Assert
        StepVerifier.create(Mono.zip(result1, result2))
                .expectNextMatches(tuple -> 
                    tuple.getT1().equals(response1) && tuple.getT2().equals(response2))
                .verifyComplete();
        
        verify(mockGetFunction, times(1)).apply(key1);
        verify(mockGetFunction, times(1)).apply(key2);
    }
}