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


package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Converter for Resilience4j exceptions.
 * Converts circuit breaker, bulkhead, rate limiter, and retry exceptions
 * to appropriate business exceptions with detailed metadata.
 * 
 * <p>This converter is only active when Resilience4j is on the classpath.</p>
 * 
 * <p>Supported exceptions:</p>
 * <ul>
 *   <li>CallNotPermittedException - Circuit breaker open</li>
 *   <li>BulkheadFullException - Bulkhead queue full</li>
 *   <li>RequestNotPermitted - Rate limiter exceeded</li>
 *   <li>TimeoutException - Operation timeout</li>
 * </ul>
 */
@Component
@ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CallNotPermittedException")
public class Resilience4jExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new Resilience4jExceptionConverter.
     */
    public Resilience4jExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        String className = exception.getClass().getName();
        return className.equals("io.github.resilience4j.circuitbreaker.CallNotPermittedException") ||
               className.equals("io.github.resilience4j.bulkhead.BulkheadFullException") ||
               className.equals("io.github.resilience4j.ratelimiter.RequestNotPermitted") ||
               className.equals("io.github.resilience4j.timelimiter.TimeoutException") ||
               (exception instanceof TimeoutException && isResilience4jTimeout(exception));
    }

    @Override
    public BusinessException convert(Exception exception) {
        String className = exception.getClass().getName();

        if (className.equals("io.github.resilience4j.circuitbreaker.CallNotPermittedException")) {
            return convertCircuitBreakerException(exception);
        } else if (className.equals("io.github.resilience4j.bulkhead.BulkheadFullException")) {
            return convertBulkheadException(exception);
        } else if (className.equals("io.github.resilience4j.ratelimiter.RequestNotPermitted")) {
            return convertRateLimiterException(exception);
        } else if (className.equals("io.github.resilience4j.timelimiter.TimeoutException") ||
                   (exception instanceof TimeoutException && isResilience4jTimeout(exception))) {
            return convertTimeoutException(exception);
        }

        // Fallback
        return new ServiceException("RESILIENCE_ERROR", exception.getMessage());
    }

    /**
     * Converts circuit breaker CallNotPermittedException to CircuitBreakerException.
     */
    private BusinessException convertCircuitBreakerException(Exception exception) {
        String message = exception.getMessage();
        String circuitBreakerName = extractCircuitBreakerName(message);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", circuitBreakerName);
        metadata.put("state", "OPEN");
        metadata.put("failureRate", extractFailureRate(message));
        metadata.put("failureRateThreshold", 50.0); // Default threshold
        metadata.put("failureCount", 0);
        metadata.put("retryAfter", 30);
        metadata.put("fallbackSuggestion", "Use cached data or retry after the circuit breaker resets");

        return new CircuitBreakerException(
                circuitBreakerName,
                "OPEN",
                extractFailureRate(message),
                50.0,
                30,
                metadata
        );
    }

    /**
     * Converts bulkhead BulkheadFullException to BulkheadException.
     */
    private BusinessException convertBulkheadException(Exception exception) {
        String message = exception.getMessage();
        String bulkheadName = extractBulkheadName(message);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", bulkheadName);
        metadata.put("maxConcurrentCalls", 100); // Default value
        metadata.put("retryAfter", 5);
        metadata.put("suggestion", "The service is at capacity. Please retry after a short delay.");

        return new BulkheadException(
                bulkheadName,
                100,
                5,
                metadata
        );
    }

    /**
     * Converts rate limiter RequestNotPermitted to RateLimitException.
     */
    private BusinessException convertRateLimiterException(Exception exception) {
        String message = exception.getMessage();
        String rateLimiterName = extractRateLimiterName(message);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", rateLimiterName);
        metadata.put("limit", 100);
        metadata.put("remaining", 0);
        metadata.put("resetTime", System.currentTimeMillis() / 1000 + 60);
        metadata.put("windowSeconds", 60);
        metadata.put("limitType", "service");
        metadata.put("retryAfter", 60);

        return new RateLimitException(
                "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded for " + rateLimiterName,
                metadata
        );
    }

    /**
     * Converts timeout exception to RetryExhaustedException or OperationTimeoutException.
     */
    private BusinessException convertTimeoutException(Exception exception) {
        String message = exception.getMessage();
        String operationName = extractOperationName(message);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("operation", operationName);
        metadata.put("timeout", 0);
        metadata.put("retryAfter", 10);

        // If this is part of a retry mechanism, use RetryExhaustedException
        if (message != null && message.toLowerCase().contains("retry")) {
            metadata.put("maxAttempts", 3);
            metadata.put("attemptsMade", 3);
            return new RetryExhaustedException(
                    operationName,
                    3,
                    10,
                    metadata
            );
        }

        // Otherwise, use OperationTimeoutException
        return OperationTimeoutException.serviceCallTimeout(
                "resilience4j",
                operationName,
                0
        );
    }

    /**
     * Checks if a TimeoutException is from Resilience4j.
     */
    private boolean isResilience4jTimeout(Throwable exception) {
        if (!(exception instanceof TimeoutException)) {
            return false;
        }
        
        // Check stack trace for Resilience4j classes
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().startsWith("io.github.resilience4j")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Extracts circuit breaker name from exception message.
     */
    private String extractCircuitBreakerName(String message) {
        if (message == null) {
            return "unknown";
        }
        
        // Try to extract name from message like "CircuitBreaker 'myService' is OPEN"
        if (message.contains("'") && message.contains("'")) {
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start + 1, end);
            }
        }
        
        return "unknown";
    }

    /**
     * Extracts bulkhead name from exception message.
     */
    private String extractBulkheadName(String message) {
        if (message == null) {
            return "unknown";
        }
        
        // Try to extract name from message
        if (message.contains("'") && message.contains("'")) {
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start + 1, end);
            }
        }
        
        return "unknown";
    }

    /**
     * Extracts rate limiter name from exception message.
     */
    private String extractRateLimiterName(String message) {
        if (message == null) {
            return "unknown";
        }
        
        // Try to extract name from message
        if (message.contains("'") && message.contains("'")) {
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start + 1, end);
            }
        }
        
        return "unknown";
    }

    /**
     * Extracts operation name from exception message.
     */
    private String extractOperationName(String message) {
        if (message == null) {
            return "unknown";
        }
        
        // Try to extract operation name
        if (message.contains("'") && message.contains("'")) {
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start + 1, end);
            }
        }
        
        return "unknown";
    }

    /**
     * Extracts failure rate from exception message.
     */
    private double extractFailureRate(String message) {
        if (message == null) {
            return 0.0;
        }
        
        // Try to extract failure rate from message
        // This is a simple implementation - in practice, you might need more sophisticated parsing
        return 0.0;
    }
}

