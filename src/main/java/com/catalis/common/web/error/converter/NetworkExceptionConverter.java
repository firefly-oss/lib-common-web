package com.catalis.common.web.error.converter;

import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.exceptions.OperationTimeoutException;
import com.catalis.common.web.error.exceptions.ThirdPartyServiceException;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Converter for network-related exceptions.
 * Converts network exceptions to appropriate business exceptions.
 */
@Component
public class NetworkExceptionConverter implements ExceptionConverter<Exception> {

    /**
     * Creates a new NetworkExceptionConverter.
     */
    public NetworkExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<Exception> getExceptionType() {
        return Exception.class;
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ConnectException ||
               exception instanceof SocketTimeoutException ||
               exception instanceof UnknownHostException;
    }

    @Override
    public BusinessException convert(Exception exception) {
        if (exception instanceof SocketTimeoutException) {
            return OperationTimeoutException.serviceCallTimeout("unknown", "network-call", 0);
        } else if (exception instanceof ConnectException) {
            return ThirdPartyServiceException.serviceUnavailable("unknown");
        } else if (exception instanceof UnknownHostException) {
            return ThirdPartyServiceException.serviceUnavailable("unknown-host");
        }

        // Fallback
        return ThirdPartyServiceException.serviceError("network", "NETWORK_ERROR", exception.getMessage());
    }
}
