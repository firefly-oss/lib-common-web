package com.catalis.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an operation times out.
 * Results in a 408 REQUEST TIMEOUT response.
 */
public class OperationTimeoutException extends BusinessException {
    
    /**
     * The operation that timed out.
     */
    private final String operation;
    
    /**
     * The timeout in milliseconds.
     */
    private final long timeoutMillis;
    
    /**
     * Creates a new OperationTimeoutException with the given message.
     *
     * @param message the error message
     * @param operation the operation that timed out
     * @param timeoutMillis the timeout in milliseconds
     */
    public OperationTimeoutException(String message, String operation, long timeoutMillis) {
        super(HttpStatus.REQUEST_TIMEOUT, "OPERATION_TIMEOUT", message);
        this.operation = operation;
        this.timeoutMillis = timeoutMillis;
    }
    
    /**
     * Creates a new OperationTimeoutException with a code and message.
     *
     * @param code the error code
     * @param message the error message
     * @param operation the operation that timed out
     * @param timeoutMillis the timeout in milliseconds
     */
    public OperationTimeoutException(String code, String message, String operation, long timeoutMillis) {
        super(HttpStatus.REQUEST_TIMEOUT, code, message);
        this.operation = operation;
        this.timeoutMillis = timeoutMillis;
    }
    
    /**
     * Returns the operation that timed out.
     *
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Returns the timeout in milliseconds.
     *
     * @return the timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }
    
    /**
     * Creates a new OperationTimeoutException for a database operation.
     *
     * @param operation the database operation that timed out
     * @param timeoutMillis the timeout in milliseconds
     * @return a new OperationTimeoutException
     */
    public static OperationTimeoutException databaseTimeout(String operation, long timeoutMillis) {
        return new OperationTimeoutException(
                "DATABASE_TIMEOUT",
                String.format("Database operation '%s' timed out after %d ms", operation, timeoutMillis),
                operation,
                timeoutMillis
        );
    }
    
    /**
     * Creates a new OperationTimeoutException for a service call.
     *
     * @param serviceName the name of the service
     * @param operation the operation that timed out
     * @param timeoutMillis the timeout in milliseconds
     * @return a new OperationTimeoutException
     */
    public static OperationTimeoutException serviceCallTimeout(String serviceName, String operation, long timeoutMillis) {
        return new OperationTimeoutException(
                "SERVICE_CALL_TIMEOUT",
                String.format("Call to service '%s' operation '%s' timed out after %d ms", serviceName, operation, timeoutMillis),
                operation,
                timeoutMillis
        );
    }
}
