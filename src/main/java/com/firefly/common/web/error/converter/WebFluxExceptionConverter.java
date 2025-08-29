package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.*;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converter for Spring WebFlux exceptions.
 * Converts WebFlux exceptions to appropriate business exceptions.
 */
@Component
public class WebFluxExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new WebFluxExceptionConverter.
     */
    public WebFluxExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MethodNotAllowedException
                || exception instanceof UnsupportedMediaTypeStatusException
                || exception instanceof NotAcceptableStatusException
                || exception instanceof DataBufferLimitException
                || (exception instanceof ServerWebInputException && ((ServerWebInputException) exception).getStatusCode() == HttpStatus.PRECONDITION_FAILED);
    }

    @Override
    public BusinessException convert(Exception exception) {
        if (exception instanceof MethodNotAllowedException) {
            MethodNotAllowedException ex = (MethodNotAllowedException) exception;
            Set<HttpMethod> allowedMethods = ex.getSupportedMethods();
            HttpMethod method = null;
            try {
                method = HttpMethod.valueOf(ex.getHttpMethod());
            } catch (Exception e) {
                // If we can't parse the method, just use null
            }

            return com.firefly.common.web.error.exceptions.MethodNotAllowedException.forResource(
                    method,
                    allowedMethods.toArray(new HttpMethod[0])
            );
        } else if (exception instanceof UnsupportedMediaTypeStatusException) {
            UnsupportedMediaTypeStatusException ex = (UnsupportedMediaTypeStatusException) exception;
            MediaType contentType = ex.getContentType();
            List<MediaType> supportedTypesList = ex.getSupportedMediaTypes();
            Set<MediaType> supportedTypes = new HashSet<>(supportedTypesList);

            return com.firefly.common.web.error.exceptions.UnsupportedMediaTypeException.forMediaType(
                    contentType != null ? contentType.toString() : "unknown",
                    supportedTypes.stream()
                            .map(MediaType::toString)
                            .toArray(String[]::new)
            );
        } else if (exception instanceof NotAcceptableStatusException) {
            NotAcceptableStatusException ex = (NotAcceptableStatusException) exception;
            List<MediaType> supportedTypesList = ex.getSupportedMediaTypes();
            Set<MediaType> supportedTypes = new HashSet<>(supportedTypesList);

            return new BusinessException(
                    HttpStatus.NOT_ACCEPTABLE,
                    "NOT_ACCEPTABLE",
                    "The requested media type is not supported. Supported types: " +
                            supportedTypes.stream()
                                    .map(MediaType::toString)
                                    .collect(Collectors.joining(", "))
            );
        } else if (exception instanceof DataBufferLimitException) {
            DataBufferLimitException ex = (DataBufferLimitException) exception;
            String message = ex.getMessage();
            long maxSize = 0;
            long actualSize = 0;

            // Try to extract sizes from the message
            if (message != null && message.contains("Exceeded limit")) {
                try {
                    String[] parts = message.split(":");
                    if (parts.length > 1) {
                        String[] sizeParts = parts[1].trim().split(" ");
                        if (sizeParts.length > 2) {
                            actualSize = Long.parseLong(sizeParts[0]);
                            maxSize = Long.parseLong(sizeParts[2]);
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing errors
                }
            }

            return com.firefly.common.web.error.exceptions.PayloadTooLargeException.forSize(maxSize, actualSize);
        } else if (exception instanceof ServerWebInputException && ((ServerWebInputException) exception).getStatusCode() == HttpStatus.PRECONDITION_FAILED) {
            ServerWebInputException ex = (ServerWebInputException) exception;
            String message = ex.getReason();

            return com.firefly.common.web.error.exceptions.PreconditionFailedException.forPrecondition(
                    "unknown",
                    message != null ? message : "Precondition failed"
            );
        }

        // Default fallback
        return new ServiceException("UNEXPECTED_ERROR", "An unexpected error occurred: " + exception.getMessage());
    }
}
