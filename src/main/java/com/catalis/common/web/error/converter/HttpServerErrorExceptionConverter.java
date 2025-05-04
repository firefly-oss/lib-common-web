package com.catalis.common.web.error.converter;

import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.exceptions.OperationTimeoutException;
import com.catalis.common.web.error.exceptions.ServiceException;
import com.catalis.common.web.error.exceptions.ThirdPartyServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Converter for Spring's HttpServerErrorException.
 * Converts HTTP server error exceptions to the appropriate business exceptions.
 */
@Component
public class HttpServerErrorExceptionConverter implements ExceptionConverter<HttpServerErrorException> {

    /**
     * Creates a new HttpServerErrorExceptionConverter.
     */
    public HttpServerErrorExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<HttpServerErrorException> getExceptionType() {
        return HttpServerErrorException.class;
    }

    @Override
    public BusinessException convert(HttpServerErrorException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String responseBody = exception.getResponseBodyAsString();

        switch (status) {
            case GATEWAY_TIMEOUT:
                return new OperationTimeoutException(
                        "GATEWAY_TIMEOUT",
                        "Gateway timeout: " + responseBody,
                        "http-request",
                        0
                );
            case BAD_GATEWAY:
                return new ThirdPartyServiceException(
                        "BAD_GATEWAY",
                        "Bad gateway: " + responseBody,
                        "unknown"
                );
            case SERVICE_UNAVAILABLE:
                return ThirdPartyServiceException.serviceUnavailable("unknown");
            default:
                return new ServiceException(
                        "SERVER_ERROR",
                        "Server error: " + responseBody
                );
        }
    }
}
