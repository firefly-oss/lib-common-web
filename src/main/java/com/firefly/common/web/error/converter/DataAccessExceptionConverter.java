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
