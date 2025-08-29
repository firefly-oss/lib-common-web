package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.BusinessException;
import com.firefly.common.web.error.exceptions.DataIntegrityException;
import com.firefly.common.web.error.exceptions.ResourceNotFoundException;
import com.firefly.common.web.error.exceptions.ServiceException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;
import org.springframework.stereotype.Component;

/**
 * Converter for JPA exceptions.
 * Converts JPA exceptions to appropriate business exceptions.
 */
@Component
public class JpaExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new JpaExceptionConverter.
     */
    public JpaExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof PersistenceException ||
               exception instanceof EntityNotFoundException ||
               exception instanceof NoResultException;
    }

    @Override
    public BusinessException convert(Exception exception) {
        if (exception instanceof EntityNotFoundException || exception instanceof NoResultException) {
            return ResourceNotFoundException.withReason("ENTITY_NOT_FOUND", exception.getMessage());
        } else if (exception instanceof EntityExistsException) {
            return DataIntegrityException.uniqueConstraintViolation("entity", "duplicate");
        } else if (exception instanceof QueryTimeoutException) {
            return com.firefly.common.web.error.exceptions.OperationTimeoutException.databaseTimeout("jpa-query", 0);
        } else if (exception instanceof PersistenceException) {
            // Try to extract more information from the message
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("unique constraint") || message.contains("duplicate")) {
                    return DataIntegrityException.uniqueConstraintViolation("unknown", message);
                } else if (message.contains("foreign key constraint")) {
                    return DataIntegrityException.foreignKeyConstraintViolation("unknown", "unknown", "unknown");
                }
            }
            return new ServiceException("DATABASE_ERROR", "Database error: " + exception.getMessage());
        }

        // Fallback
        return new ServiceException("JPA_ERROR", "JPA error: " + exception.getMessage());
    }
}
