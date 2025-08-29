package com.firefly.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a data integrity constraint is violated.
 * Results in a 400 BAD REQUEST response.
 */
public class DataIntegrityException extends BusinessException {
    
    /**
     * Creates a new DataIntegrityException with the given message.
     *
     * @param message the error message
     */
    public DataIntegrityException(String message) {
        super(HttpStatus.BAD_REQUEST, "DATA_INTEGRITY_VIOLATION", message);
    }
    
    /**
     * Creates a new DataIntegrityException with a code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public DataIntegrityException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
    
    /**
     * Creates a new DataIntegrityException for a unique constraint violation.
     *
     * @param field the field that must be unique
     * @param value the value that violates the constraint
     * @return a new DataIntegrityException
     */
    public static DataIntegrityException uniqueConstraintViolation(String field, String value) {
        return new DataIntegrityException(
                "UNIQUE_CONSTRAINT_VIOLATION",
                String.format("The value '%s' for field '%s' already exists", value, field)
        );
    }
    
    /**
     * Creates a new DataIntegrityException for a foreign key constraint violation.
     *
     * @param field the field that references another entity
     * @param value the value that violates the constraint
     * @param referencedEntity the entity that is referenced
     * @return a new DataIntegrityException
     */
    public static DataIntegrityException foreignKeyConstraintViolation(String field, String value, String referencedEntity) {
        return new DataIntegrityException(
                "FOREIGN_KEY_CONSTRAINT_VIOLATION",
                String.format("The value '%s' for field '%s' does not reference a valid %s", value, field, referencedEntity)
        );
    }
    
    /**
     * Creates a new DataIntegrityException for a check constraint violation.
     *
     * @param field the field that violates the constraint
     * @param value the value that violates the constraint
     * @param constraint the constraint that is violated
     * @return a new DataIntegrityException
     */
    public static DataIntegrityException checkConstraintViolation(String field, String value, String constraint) {
        return new DataIntegrityException(
                "CHECK_CONSTRAINT_VIOLATION",
                String.format("The value '%s' for field '%s' violates the constraint: %s", value, field, constraint)
        );
    }
}
