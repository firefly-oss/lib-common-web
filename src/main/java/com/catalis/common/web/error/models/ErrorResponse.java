package com.catalis.common.web.error.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response model used across the application.
 * This class represents the structure of error responses returned to clients.
 * It includes details such as timestamp, HTTP status, error message, and validation errors.
 * Also provides additional context like suggestions, documentation links, and detailed error information.
 *
 * The class uses Lombok's Builder and Data annotations to generate boilerplate code.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Default constructor for ErrorResponse.
     * This constructor is required for Jackson deserialization and Lombok's @Builder annotation.
     * It is not meant to be used directly - use the builder pattern instead.
     */
    @SuppressWarnings("unused")
    private ErrorResponse() {
        // Default constructor for Jackson and Lombok
    }

    /**
     * All-args constructor for ErrorResponse.
     * This constructor is used by Lombok's @Builder annotation.
     * It is not meant to be used directly - use the builder pattern instead.
     *
     * @param timestamp the timestamp when the error occurred
     * @param path the request path that caused the error
     * @param status the HTTP status code
     * @param error the error type
     * @param message the error message
     * @param code the error code
     * @param traceId the trace ID for the request
     * @param details additional details about the error
     * @param suggestion a suggestion for how to fix the error
     * @param documentation a link to documentation about the error
     * @param metadata additional metadata about the error
     * @param errors a list of validation errors
     */
    @SuppressWarnings("unused")
    @lombok.Builder
    private ErrorResponse(LocalDateTime timestamp, String path, Integer status, String error,
                         String message, String code, String traceId, String details,
                         String suggestion, String documentation, Map<String, Object> metadata,
                         List<ValidationError> errors) {
        this.timestamp = timestamp;
        this.path = path;
        this.status = status;
        this.error = error;
        this.message = message;
        this.code = code;
        this.traceId = traceId;
        this.details = details;
        this.suggestion = suggestion;
        this.documentation = documentation;
        this.metadata = metadata;
        this.errors = errors;
    }

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd/MM/yyyy'T'HH:mm:ss.SSSSSS",
            timezone = "Europe/Madrid"
    )
    private LocalDateTime timestamp;

    private String path;
    private Integer status;
    private String error;
    private String message;
    private String traceId;
    private String code;

    /**
     * Additional details about the error.
     * This can include technical information that might be useful for debugging.
     */
    private String details;

    /**
     * A suggestion for how to resolve the error.
     * This provides guidance to the client on how to fix the issue.
     */
    private String suggestion;

    /**
     * A link to documentation that provides more information about the error.
     * This can be a URL to API documentation or a knowledge base article.
     */
    private String documentation;

    /**
     * Additional metadata about the error.
     * This can include any additional information that might be useful for debugging or understanding the error.
     */
    private Map<String, Object> metadata;

    /**
     * A list of validation errors.
     * This is used when the error is related to validation failures for multiple fields.
     */
    private List<ValidationError> errors;

    /**
     * Represents a field-specific validation error.
     * This class is used to provide detailed information about validation failures
     * for specific fields in a request.
     *
     * The class uses Lombok's Builder and Data annotations to generate boilerplate code.
     */
    @Data
    public static class ValidationError {
        /**
         * Default constructor for ValidationError.
         * This constructor is required for Jackson deserialization and Lombok's @Builder annotation.
         * It is not meant to be used directly - use the builder pattern instead.
         */
        @SuppressWarnings("unused")
        private ValidationError() {
            // Default constructor for Jackson and Lombok
        }

        /**
         * All-args constructor for ValidationError.
         * This constructor is used by Lombok's @Builder annotation.
         * It is not meant to be used directly - use the builder pattern instead.
         *
         * @param field the field that failed validation
         * @param code the error code for this validation error
         * @param message the validation error message
         * @param metadata additional metadata about the validation error
         */
        @SuppressWarnings("unused")
        @lombok.Builder
        private ValidationError(String field, String code, String message, Map<String, Object> metadata) {
            this.field = field;
            this.code = code;
            this.message = message;
            this.metadata = metadata;
        }
        /**
         * The field that failed validation.
         */
        private String field;

        /**
         * The error code for this validation error.
         * This can be used to identify the specific validation rule that failed.
         */
        private String code;

        /**
         * The validation error message.
         * This provides a human-readable description of the validation failure.
         */
        private String message;

        /**
         * Additional metadata about the validation error.
         * This can include information such as allowed values, min/max values, etc.
         */
        private Map<String, Object> metadata;
    }
}

