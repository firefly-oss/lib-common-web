package com.firefly.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request conflicts with the current state of the server.
 * Results in a 409 CONFLICT response.
 */
public class ConflictException extends BusinessException {
    
    /**
     * Creates a new ConflictException with the given message.
     *
     * @param message the error message
     */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
    
    /**
     * Creates a new ConflictException with a code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
    
    /**
     * Creates a new ConflictException for a resource that already exists.
     *
     * @param resourceType the type of resource that already exists
     * @param identifier the identifier of the resource that already exists
     * @return a new ConflictException
     */
    public static ConflictException resourceAlreadyExists(String resourceType, String identifier) {
        return new ConflictException(
                "RESOURCE_ALREADY_EXISTS",
                String.format("%s with identifier '%s' already exists", resourceType, identifier)
        );
    }
}
