package com.firefly.common.web.error.converter;

import com.firefly.common.web.error.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Converter for Spring's HttpClientErrorException.
 * Converts HTTP client error exceptions to the appropriate business exceptions.
 */
@Component
public class HttpClientErrorExceptionConverter implements ExceptionConverter<HttpClientErrorException> {

    /**
     * Creates a new HttpClientErrorExceptionConverter.
     */
    public HttpClientErrorExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<HttpClientErrorException> getExceptionType() {
        return HttpClientErrorException.class;
    }

    @Override
    public BusinessException convert(HttpClientErrorException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String responseBody = exception.getResponseBodyAsString();

        switch (status) {
            case NOT_FOUND:
                return new ResourceNotFoundException(
                        "RESOURCE_NOT_FOUND",
                        "The requested resource was not found: " + responseBody
                );
            case UNAUTHORIZED:
                return new UnauthorizedException(
                        "AUTHENTICATION_REQUIRED",
                        "Authentication is required: " + responseBody
                );
            case FORBIDDEN:
                return new ForbiddenException(
                        "ACCESS_DENIED",
                        "Access denied: " + responseBody
                );
            case BAD_REQUEST:
                return new InvalidRequestException(
                        "INVALID_REQUEST",
                        "Invalid request: " + responseBody
                );
            case CONFLICT:
                return new ConflictException(
                        "RESOURCE_CONFLICT",
                        "Resource conflict: " + responseBody
                );
            case TOO_MANY_REQUESTS:
                Integer retryAfter = null;
                if (exception.getResponseHeaders() != null && exception.getResponseHeaders().getFirst("Retry-After") != null) {
                    try {
                        retryAfter = Integer.parseInt(exception.getResponseHeaders().getFirst("Retry-After"));
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                return new RateLimitException(
                        "RATE_LIMIT_EXCEEDED",
                        "Rate limit exceeded: " + responseBody,
                        retryAfter
                );
            case REQUEST_TIMEOUT:
                return new OperationTimeoutException(
                        "REQUEST_TIMEOUT",
                        "Request timed out: " + responseBody,
                        "http-request",
                        0
                );
            default:
                return new BusinessException(
                        status,
                        "HTTP_CLIENT_ERROR",
                        "HTTP client error: " + responseBody
                );
        }
    }
}
