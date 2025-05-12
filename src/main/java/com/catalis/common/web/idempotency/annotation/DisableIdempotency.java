package com.catalis.common.web.idempotency.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to disable idempotency for specific endpoints.
 * When applied to a controller method, the idempotency filter will not
 * process requests to that endpoint, and the X-Idempotency-Key header
 * will not be included in the OpenAPI documentation for that operation.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @PostMapping("/api/resource")
 * @DisableIdempotency
 * public Mono<ResponseEntity<Resource>> createResource(@RequestBody Resource resource) {
 *     // Method implementation
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DisableIdempotency {
}
