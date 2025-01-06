package com.catalis.common.web.error.handler;

import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.models.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Hidden
@Order(-2)
@Configuration
@RestControllerAdvice
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return handleBusinessException(exchange, businessException);
        }
        if (ex instanceof WebExchangeBindException validationException) {
            return handleValidationException(exchange, validationException);
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return handleResponseStatusException(exchange, responseStatusException);
        }
        return handleUnexpectedError(exchange, ex);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private Mono<Void> handleBusinessException(ServerWebExchange exchange, BusinessException ex) {
        log.error("Business exception occurred: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(exchange.getRequest().getPath().value())
                .build();

        exchange.getResponse().setStatusCode(ex.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private Mono<Void> handleValidationException(ServerWebExchange exchange, WebExchangeBindException ex) {
        log.error("Validation exception occurred: ", ex);

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String field = error instanceof FieldError fieldError ?
                            fieldError.getField() : error.getObjectName();
                    return ErrorResponse.ValidationError.builder()
                            .field(field)
                            .message(error.getDefaultMessage())
                            .build();
                })
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .path(exchange.getRequest().getPath().value())
                .errors(validationErrors)
                .build();

        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    private Mono<Void> handleResponseStatusException(ServerWebExchange exchange, ResponseStatusException ex) {
        log.error("Response status exception occurred: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().toString())
                .message(ex.getReason())
                .path(exchange.getRequest().getPath().value())
                .build();

        exchange.getResponse().setStatusCode(ex.getStatusCode());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    private Mono<Void> handleUnexpectedError(ServerWebExchange exchange, Throwable ex) {
        log.error("Unexpected error occurred: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred: " + ex.getMessage())
                .path(exchange.getRequest().getPath().value())
                .build();

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    private String toJson(ErrorResponse errorResponse) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .findAndRegisterModules()
                    .writeValueAsString(errorResponse);
        } catch (Exception e) {
            log.error("Error converting ErrorResponse to JSON", e);
            return "{\"message\":\"Error processing response\"}";
        }
    }
}