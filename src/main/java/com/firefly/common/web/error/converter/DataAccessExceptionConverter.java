package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.BusinessException;
import com.firefly.common.web.error.exceptions.DataIntegrityException;
import com.firefly.common.web.error.exceptions.OperationTimeoutException;
import com.firefly.common.web.error.exceptions.ServiceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

/**
 * Converter for Spring's DataAccessException.
 * Converts various data access exceptions to the appropriate business exceptions.
 */
@Component
public class DataAccessExceptionConverter implements ExceptionConverter<DataAccessException> {

    /**
     * Creates a new DataAccessExceptionConverter.
     */
    public DataAccessExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<DataAccessException> getExceptionType() {
        return DataAccessException.class;
    }

    @Override
    public BusinessException convert(DataAccessException exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return handleDataIntegrityViolationException((DataIntegrityViolationException) exception);
        } else if (exception instanceof QueryTimeoutException) {
            return handleQueryTimeoutException((QueryTimeoutException) exception);
        } else if (exception instanceof TransientDataAccessException) {
            return handleTransientDataAccessException((TransientDataAccessException) exception);
        } else {
            return new ServiceException(
                    "DATABASE_ERROR",
                    "A database error occurred: " + exception.getMessage()
            );
        }
    }

    private BusinessException handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        String message = exception.getMessage();

        // Try to extract constraint information from the message
        if (message != null) {
            if (message.contains("unique constraint") || message.contains("Duplicate entry")) {
                return DataIntegrityException.uniqueConstraintViolation("unknown", message);
            } else if (message.contains("foreign key constraint") || message.contains("referenced row")) {
                return DataIntegrityException.foreignKeyConstraintViolation("unknown", "unknown", "unknown");
            } else if (message.contains("check constraint")) {
                return DataIntegrityException.checkConstraintViolation("unknown", "unknown", "unknown");
            }
        }

        return new DataIntegrityException(
                "DATA_INTEGRITY_VIOLATION",
                "A data integrity violation occurred: " + message
        );
    }

    private BusinessException handleQueryTimeoutException(QueryTimeoutException exception) {
        return OperationTimeoutException.databaseTimeout("query", 0);
    }

    private BusinessException handleTransientDataAccessException(TransientDataAccessException exception) {
        return new ServiceException(
                "TRANSIENT_DATABASE_ERROR",
                "A temporary database error occurred: " + exception.getMessage()
        );
    }
}
