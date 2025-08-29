package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.BusinessException;
import com.firefly.common.web.error.exceptions.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;

/**
 * Converter for Spring's validation exceptions.
 * Converts validation exceptions to ValidationException.
 */
@Component
public class ValidationExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new ValidationExceptionConverter.
     */
    public ValidationExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MethodArgumentNotValidException ||
               exception instanceof BindException ||
               exception instanceof WebExchangeBindException;
    }

    @Override
    public BusinessException convert(Exception exception) {
        BindingResult bindingResult = null;

        if (exception instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) exception).getBindingResult();
        } else if (exception instanceof BindException) {
            bindingResult = ((BindException) exception);
        } else if (exception instanceof WebExchangeBindException) {
            bindingResult = ((WebExchangeBindException) exception);
        }

        if (bindingResult != null) {
            ValidationException.Builder builder = new ValidationException.Builder();

            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                builder.addError(fieldError.getField(), fieldError.getDefaultMessage());
            }

            return builder.build();
        }

        // Fallback
        return new ValidationException("Validation failed");
    }
}
