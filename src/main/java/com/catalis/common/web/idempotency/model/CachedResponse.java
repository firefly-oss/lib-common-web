package com.catalis.common.web.idempotency.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

/**
 * Data Transfer Object (DTO) for storing cached HTTP responses.
 * Used by the idempotency mechanism to store and retrieve responses
 * for requests with the same idempotency key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedResponse {
    
    /**
     * HTTP status code of the cached response
     */
    private int status;
    
    /**
     * Response body as byte array
     */
    private byte[] body;
    
    /**
     * Content type of the response
     */
    private MediaType contentType;
}