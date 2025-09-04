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
import io.r2dbc.spi.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Converter for R2DBC exceptions.
 * Converts R2DBC exceptions to appropriate business exceptions.
 */
@Component
public class R2dbcExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new R2dbcExceptionConverter.
     */
    public R2dbcExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof R2dbcException;
    }

    @Override
    public BusinessException convert(Exception exception) {
        if (!(exception instanceof R2dbcException)) {
            return new ServiceException("R2DBC_ERROR", "Unexpected R2DBC error: " + exception.getMessage());
        }

        R2dbcException r2dbcException = (R2dbcException) exception;
        String sqlState = r2dbcException.getSqlState();
        int errorCode = r2dbcException.getErrorCode();
        String message = r2dbcException.getMessage();

        // Handle transient exceptions (can be retried)
        if (r2dbcException instanceof R2dbcTransientException) {
            if (r2dbcException instanceof R2dbcTimeoutException) {
                return OperationTimeoutException.databaseTimeout("r2dbc-query", 0);
            } else if (r2dbcException instanceof R2dbcRollbackException) {
                return ConcurrencyException.optimisticLockingFailure("database", String.valueOf(errorCode));
            } else {
                // Generic transient resource exception
                return ThirdPartyServiceException.serviceUnavailable("database");
            }
        }

        // Handle non-transient exceptions (should not be retried)
        if (r2dbcException instanceof R2dbcNonTransientException) {
            if (r2dbcException instanceof R2dbcDataIntegrityViolationException) {
                // Try to extract more information from the message
                if (message != null) {
                    if (message.contains("unique constraint") || message.contains("duplicate")) {
                        return DataIntegrityException.uniqueConstraintViolation("unknown", message);
                    } else if (message.contains("foreign key constraint")) {
                        return DataIntegrityException.foreignKeyConstraintViolation("unknown", "unknown", "unknown");
                    }
                }
                return new DataIntegrityException("DATA_INTEGRITY_VIOLATION", "Database integrity violation: " + message);
            } else if (r2dbcException instanceof R2dbcPermissionDeniedException) {
                return new ForbiddenException("DATABASE_PERMISSION_DENIED", "Database permission denied: " + message);
            } else if (r2dbcException instanceof R2dbcBadGrammarException) {
                return InvalidRequestException.withReason("SQL_SYNTAX_ERROR", "Invalid SQL syntax: " + message);
            } else {
                // Generic non-transient resource exception
                return new ServiceException("DATABASE_ERROR", "Database error: " + message);
            }
        }

        // Handle generic R2DBC exceptions based on SQL state if available
        if (sqlState != null) {
            // Connection-related errors
            if (sqlState.startsWith("08")) {
                return ThirdPartyServiceException.serviceUnavailable("database");
            }
            // Data integrity errors
            else if (sqlState.startsWith("23")) {
                if (message != null && (message.contains("unique") || message.contains("duplicate"))) {
                    return DataIntegrityException.uniqueConstraintViolation("unknown", message);
                }
                return new DataIntegrityException("DATA_INTEGRITY_VIOLATION", "Database integrity violation: " + message);
            }
            // Authentication errors
            else if (sqlState.startsWith("28")) {
                return new UnauthorizedException("DATABASE_AUTH_FAILED", "Database authentication failed");
            }
            // Syntax errors
            else if (sqlState.startsWith("42")) {
                return InvalidRequestException.withReason("SQL_SYNTAX_ERROR", "Invalid SQL syntax: " + message);
            }
        }

        // Default fallback
        return new ServiceException("R2DBC_ERROR", "R2DBC database error: " + message);
    }
}
