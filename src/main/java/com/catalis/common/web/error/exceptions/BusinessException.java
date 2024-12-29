package com.catalis.common.web.error.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, null, message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}

class InvalidRequestException extends BusinessException {
    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}

class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}