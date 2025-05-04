package com.catalis.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Results in a 404 NOT FOUND response.
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Creates a new ResourceNotFoundException with the given message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Creates a new ResourceNotFoundException with a code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    /**
     * Creates a new ResourceNotFoundException for a specific resource type and ID.
     *
     * @param resourceType the type of resource that was not found
     * @param resourceId the ID of the resource that was not found
     * @return a new ResourceNotFoundException
     */
    public static ResourceNotFoundException forResource(String resourceType, String resourceId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                String.format("%s with id '%s' not found", resourceType, resourceId)
        );
    }

    /**
     * Creates a new ResourceNotFoundException with a specific reason code and message.
     *
     * @param code the error code
     * @param message the error message
     * @return a new ResourceNotFoundException
     */
    public static ResourceNotFoundException withReason(String code, String message) {
        return new ResourceNotFoundException(code, message);
    }
}
