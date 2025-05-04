package com.catalis.common.web.error.converter;

import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.exceptions.ConcurrencyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Converter for Spring's OptimisticLockingFailureException.
 * Converts optimistic locking failure exceptions to concurrency exceptions.
 */
@Component
public class OptimisticLockingFailureExceptionConverter implements ExceptionConverter<OptimisticLockingFailureException> {

    /**
     * Creates a new OptimisticLockingFailureExceptionConverter.
     */
    public OptimisticLockingFailureExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<OptimisticLockingFailureException> getExceptionType() {
        return OptimisticLockingFailureException.class;
    }

    @Override
    public BusinessException convert(OptimisticLockingFailureException exception) {
        String message = exception.getMessage();

        // Try to extract entity information from the message
        String resource = "unknown";
        String resourceId = "unknown";

        if (message != null) {
            // Try to parse entity information from the message
            // This is a simple example and might need to be adapted to your specific exception messages
            if (message.contains("entity") && message.contains("with id")) {
                int entityStart = message.indexOf("entity") + 7;
                int entityEnd = message.indexOf("with id") - 1;
                if (entityStart < entityEnd) {
                    resource = message.substring(entityStart, entityEnd).trim();
                }

                int idStart = message.indexOf("with id") + 8;
                int idEnd = message.indexOf("was", idStart);
                if (idStart < idEnd) {
                    resourceId = message.substring(idStart, idEnd).trim();
                    // Remove quotes if present
                    if (resourceId.startsWith("'") && resourceId.endsWith("'")) {
                        resourceId = resourceId.substring(1, resourceId.length() - 1);
                    }
                }
            }
        }

        return ConcurrencyException.optimisticLockingFailure(resource, resourceId);
    }
}
