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


package com.firefly.common.web.idempotency.filter;

import com.firefly.common.web.TestApplication;
import com.firefly.common.web.idempotency.annotation.DisableIdempotency;
import com.firefly.common.web.idempotency.config.IdempotencyProperties;
import com.firefly.common.web.idempotency.config.InMemoryIdempotencyConfig;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration tests for the idempotency functionality.
 * These tests verify that the idempotency mechanism works correctly in a real application
 * by making requests to a test controller and checking that duplicate requests with the same
 * idempotency key are handled correctly.
 */
@SpringBootTest(
    classes = {
        TestApplication.class, 
        IdempotencyIntegrationTest.TestConfig.class,
        InMemoryIdempotencyConfig.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.autoconfigure.exclude=com.firefly.common.web.openapi.OpenAPIConfiguration"}
)
@AutoConfigureWebTestClient
class IdempotencyIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestController testController;

    @Test
    void shouldProcessFirstRequestAndReturnCachedResponseForSecondRequest() {
        // Reset the counter
        testController.resetCounter();

        // Create a request body
        String requestBody = "{\"message\":\"test\"}";

        // Make the first request with an idempotency key
        String idempotencyKey = "integration-test-key-1";
        WebTestClient.ResponseSpec firstResponse = webTestClient
                .post()
                .uri("/test")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the first response
        firstResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #1");

        // Make a second request with the same idempotency key
        WebTestClient.ResponseSpec secondResponse = webTestClient
                .post()
                .uri("/test")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the second response is the same as the first
        secondResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #1");

        // Verify that the controller method was only called once
        assert testController.getCounter() == 1 : "Controller method should be called exactly once";
    }

    @Test
    void shouldProcessBothRequestsWithDifferentIdempotencyKeys() {
        // Reset the counter
        testController.resetCounter();

        // Create a request body
        String requestBody = "{\"message\":\"test\"}";

        // Make the first request with an idempotency key
        String firstIdempotencyKey = "integration-test-key-2";
        WebTestClient.ResponseSpec firstResponse = webTestClient
                .post()
                .uri("/test")
                .header("X-Idempotency-Key", firstIdempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the first response
        firstResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #1");

        // Make a second request with a different idempotency key
        String secondIdempotencyKey = "integration-test-key-3";
        WebTestClient.ResponseSpec secondResponse = webTestClient
                .post()
                .uri("/test")
                .header("X-Idempotency-Key", secondIdempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the second response is different
        secondResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #2");

        // Verify that the controller method was called twice
        assert testController.getCounter() == 2 : "Controller method should be called exactly twice";
    }

    @Test
    void shouldProcessBothRequestsWhenIdempotencyIsDisabled() {
        // Reset the counter
        testController.resetCounter();

        // Create a request body
        String requestBody = "{\"message\":\"test\"}";

        // Make the first request with an idempotency key
        String idempotencyKey = "integration-test-key-4";
        WebTestClient.ResponseSpec firstResponse = webTestClient
                .post()
                .uri("/test-disabled")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the first response
        firstResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #1");

        // Make a second request with the same idempotency key
        WebTestClient.ResponseSpec secondResponse = webTestClient
                .post()
                .uri("/test-disabled")
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();

        // Verify the second response is different
        secondResponse
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Processed request #2");

        // Verify that the controller method was called twice
        assert testController.getCounter() == 2 : "Controller method should be called exactly twice";
    }

    /**
     * Test configuration that provides a test controller for the integration tests.
     */
    @Test
    void shouldDeduplicateConcurrentRequestsWithSameIdempotencyKey() {
        // Reset the counter
        testController.resetCounter();

        String requestBody = "{\"message\":\"test\"}";
        String idempotencyKey = "integration-test-key-concurrent";

        CompletableFuture<String> f1 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                webTestClient
                        .post()
                        .uri("/test-slow")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(requestBody))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        );

        CompletableFuture<String> f2 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                webTestClient
                        .post()
                        .uri("/test-slow")
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(requestBody))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody()
        );

        String body1 = f1.join();
        String body2 = f2.join();

        assert body1 != null && body2 != null : "Responses should not be null";
        assert body1.equals(body2) : "Responses should be identical";
        assert body1.contains("\"message\":\"Processed request #1\"") : "Response should be from first processed request";

        assert testController.getCounter() == 1 : "Controller method should be called exactly once";
    }

    @Configuration
    static class TestConfig {

        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    /**
     * Test controller that provides endpoints for testing idempotency.
     */
    @RestController
    static class TestController {

        private final AtomicInteger counter = new AtomicInteger(0);

        @PostMapping("/test")
        public Mono<Response> handleRequest(@RequestBody Request request) {
            int count = counter.incrementAndGet();
            return Mono.just(new Response("Processed request #" + count));
        }

        @PostMapping("/test-disabled")
        @DisableIdempotency
        public Mono<Response> handleRequestWithDisabledIdempotency(@RequestBody Request request) {
            int count = counter.incrementAndGet();
            return Mono.just(new Response("Processed request #" + count));
        }

        @PostMapping("/test-slow")
        public Mono<Response> handleSlowRequest(@RequestBody Request request) {
            return Mono.delay(Duration.ofMillis(300))
                    .flatMap(t -> {
                        int count = counter.incrementAndGet();
                        return Mono.just(new Response("Processed request #" + count));
                    });
        }

        public int getCounter() {
            return counter.get();
        }

        public void resetCounter() {
            counter.set(0);
        }
    }

    /**
     * Request DTO for the test controller.
     */
    static class Request {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Response DTO for the test controller.
     */
    static class Response {
        private final String message;

        public Response(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
