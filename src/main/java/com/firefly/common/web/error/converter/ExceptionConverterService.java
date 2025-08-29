package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.BusinessException;
import com.firefly.common.web.error.exceptions.ServiceException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for converting standard exceptions to business exceptions.
 * This service uses a list of exception converters to convert exceptions
 * to the appropriate business exception.
 */
@Service
public class ExceptionConverterService {

    private final List<ExceptionConverter<?>> converters;

    /**
     * Creates a new ExceptionConverterService with the given converters.
     *
     * @param converters the list of exception converters
     */
    public ExceptionConverterService(List<ExceptionConverter<?>> converters) {
        this.converters = converters;
    }

    /**
     * Converts the given exception to a business exception.
     * If the exception is already a business exception, it is returned as is.
     * If no converter can handle the exception, a generic ServiceException is returned.
     *
     * @param exception the exception to convert
     * @return the converted business exception
     */
    public BusinessException convertException(Throwable exception) {
        // If the exception is already a business exception, return it
        if (exception instanceof BusinessException) {
            return (BusinessException) exception;
        }

        // Try to find a converter that can handle the exception
        for (ExceptionConverter<?> converter : converters) {
            if (converter.canHandle(exception)) {
                return convertWithConverter(converter, exception);
            }
        }

        // If no converter can handle the exception, return a generic service exception
        return new ServiceException(
                "UNEXPECTED_ERROR",
                "An unexpected error occurred: " + exception.getMessage()
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> BusinessException convertWithConverter(ExceptionConverter<T> converter, Throwable exception) {
        return converter.convert((T) exception);
    }
}
