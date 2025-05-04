package com.catalis.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication is required but not provided.
 * Results in a 401 UNAUTHORIZED response.
 */
public class UnauthorizedException extends BusinessException {

    /**
     * Creates a new UnauthorizedException with the given message.
     *
     * @param message the error message
     */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Creates a new UnauthorizedException with a code and message.
     *
     * @param code the error code
     * @param message the error message
     */
    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    /**
     * Creates a new UnauthorizedException for missing authentication.
     *
     * @return a new UnauthorizedException
     */
    public static UnauthorizedException missingAuthentication() {
        return new UnauthorizedException(
                "AUTHENTICATION_REQUIRED",
                "Authentication is required to access this resource"
        );
    }

    /**
     * Creates a new UnauthorizedException for invalid credentials.
     *
     * @return a new UnauthorizedException
     */
    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(
                "INVALID_CREDENTIALS",
                "The provided credentials are invalid"
        );
    }

    /**
     * Creates a new UnauthorizedException with a specific reason code and message.
     *
     * @param code the error code
     * @param message the error message
     * @return a new UnauthorizedException
     */
    public static UnauthorizedException withReason(String code, String message) {
        return new UnauthorizedException(code, message);
    }
}
