package com.catalis.common.web.error.converter;

import com.catalis.common.web.error.exceptions.BusinessException;

/**
 * Interface for converting standard exceptions to business exceptions.
 * Implementations of this interface can convert specific exception types
 * to the appropriate business exception.
 *
 * @param <T> the type of exception that this converter can handle
 */
public interface ExceptionConverter<T extends Throwable> {
    
    /**
     * Returns the exception type that this converter can handle.
     *
     * @return the exception class
     */
    Class<T> getExceptionType();
    
    /**
     * Converts the given exception to a business exception.
     *
     * @param exception the exception to convert
     * @return the converted business exception
     */
    BusinessException convert(T exception);
    
    /**
     * Returns whether this converter can handle the given exception.
     *
     * @param exception the exception to check
     * @return true if this converter can handle the exception, false otherwise
     */
    default boolean canHandle(Throwable exception) {
        return getExceptionType().isInstance(exception);
    }
}
