package com.catalis.common.web.error.handler;

import com.catalis.common.web.error.converter.ExceptionConverterService;
import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.exceptions.ValidationException;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for the application.
 * This class handles all exceptions thrown by the application and converts them
 * into standardized error responses. It implements Spring's ErrorWebExceptionHandler
 * to provide consistent error handling across the application.
 */
@Slf4j
@Hidden
@Order(-2)
@Configuration
@RestControllerAdvice
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ExceptionConverterService converterService;

    /**
     * Creates a new GlobalExceptionHandler with the given converter service.
     *
     * @param converterService the service used to convert exceptions to business exceptions
     */
    public GlobalExceptionHandler(ExceptionConverterService converterService) {
        this.converterService = converterService;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // If the exception is already a BusinessException, handle it directly
        if (ex instanceof BusinessException businessException) {
            return handleBusinessException(exchange, businessException);
        }

        // For ValidationException, use the specialized handler
        if (ex instanceof ValidationException validationException) {
            return handleCustomValidationException(exchange, validationException);
        }

        // For WebExchangeBindException, use the specialized handler
        if (ex instanceof WebExchangeBindException validationException) {
            return handleValidationException(exchange, validationException);
        }

        // For ResponseStatusException, use the specialized handler
        if (ex instanceof ResponseStatusException responseStatusException) {
            return handleResponseStatusException(exchange, responseStatusException);
        }

        // For all other exceptions, try to convert them to a BusinessException
        try {
            BusinessException convertedException = converterService.convertException(ex);
            return handleBusinessException(exchange, convertedException);
        } catch (Exception conversionError) {
            // If conversion fails, handle as an unexpected error
            log.error("Error converting exception", conversionError);
            return handleUnexpectedError(exchange, ex);
        }
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private Mono<Void> handleBusinessException(ServerWebExchange exchange, BusinessException ex) {
        log.error("Business exception occurred: ", ex);

        // Create a more detailed error response with additional context
        var builder = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(exchange.getRequest().getPath().value())
                .traceId(UUID.randomUUID().toString())
                .code(ex.getCode())
                .metadata(ex.getMetadata());

        // Add suggestion from metadata if available, otherwise use default suggestions
        Map<String, Object> metadata = ex.getMetadata();
        if (metadata != null && metadata.containsKey("suggestion")) {
            builder.suggestion(metadata.get("suggestion").toString());
        } else {
            // Add suggestion based on the exception type
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                builder.suggestion("Verify the resource identifier and ensure it exists.");
            } else if (ex.getStatus() == HttpStatus.BAD_REQUEST) {
                builder.suggestion("Check your request parameters and try again.");
            } else if (ex.getStatus() == HttpStatus.UNAUTHORIZED) {
                builder.suggestion("Please authenticate and try again.");
            } else if (ex.getStatus() == HttpStatus.FORBIDDEN) {
                builder.suggestion("You don't have permission to access this resource. Contact an administrator if you need access.");
            } else if (ex.getStatus() == HttpStatus.CONFLICT) {
                builder.suggestion("The request conflicts with the current state of the resource. Refresh and try again.");
            } else if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS) {
                builder.suggestion("You've exceeded the rate limit. Please wait and try again later.");
            } else if (ex.getStatus() == HttpStatus.PAYLOAD_TOO_LARGE) {
                builder.suggestion("The request payload is too large. Try reducing the size of your request.");
            } else if (ex.getStatus() == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                builder.suggestion("The request format is not supported. Check the Content-Type header.");
            } else if (ex.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                builder.suggestion("An unexpected error occurred. Please contact support with the trace ID.");
            }
        }

        // Add documentation link if available
        String docBase = "https://api.example.com/docs/errors/";
        if (ex.getCode() != null) {
            builder.documentation(docBase + ex.getCode().toLowerCase());
        }

        // Add technical details for non-production environments
        if (ex.getCause() != null) {
            builder.details("Caused by: " + ex.getCause().getMessage());
        }

        ErrorResponse errorResponse = builder.build();

        exchange.getResponse().setStatusCode(ex.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private Mono<Void> handleCustomValidationException(ServerWebExchange exchange, ValidationException ex) {
        log.error("Validation exception occurred: ", ex);

        // Convert validation errors to the enhanced format
        List<ErrorResponse.ValidationError> validationErrors = ex.getValidationErrors().stream()
                .map(error -> ErrorResponse.ValidationError.builder()
                        .field(error.getField())
                        .code(error.getCode())
                        .message(error.getMessage())
                        .metadata(error.getMetadata())
                        .build())
                .collect(Collectors.toList());

        // Create a more detailed error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(exchange.getRequest().getPath().value())
                .traceId(UUID.randomUUID().toString())
                .code(ex.getCode())
                .errors(validationErrors)
                .metadata(ex.getMetadata())
                .suggestion("Please check the validation errors and correct your request.")
                .documentation("https://api.example.com/docs/errors/validation")
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

        // Convert Spring validation errors to our enhanced format
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String field = error instanceof FieldError fieldError ?
                            fieldError.getField() : error.getObjectName();
                    String code = error.getCode();

                    // Create validation error with the basic information
                    Map<String, Object> metadata = new HashMap<>();

                    // Add additional metadata for specific validation errors
                    if (error instanceof FieldError fieldError) {
                        if (fieldError.getRejectedValue() != null) {
                            metadata.put("rejectedValue", fieldError.getRejectedValue().toString());
                            metadata.put("bindingFailure", fieldError.isBindingFailure());
                        }
                    }

                    return ErrorResponse.ValidationError.builder()
                            .field(field)
                            .code(code)
                            .message(error.getDefaultMessage())
                            .metadata(metadata.isEmpty() ? null : metadata)
                            .build();
                })
                .collect(Collectors.toList());

        // Create a more detailed error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .path(exchange.getRequest().getPath().value())
                .traceId(UUID.randomUUID().toString())
                .code("VALIDATION_ERROR")
                .errors(validationErrors)
                .suggestion("Please check the validation errors and correct your request.")
                .documentation("https://api.example.com/docs/errors/validation")
                .details("The request failed validation. Check the 'errors' field for details.")
                .build();

        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    private Mono<Void> handleResponseStatusException(ServerWebExchange exchange, ResponseStatusException ex) {
        log.error("Response status exception occurred: ", ex);

        // Create a more detailed error response
        var builder = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().toString())
                .message(ex.getReason())
                .path(exchange.getRequest().getPath().value())
                .traceId(UUID.randomUUID().toString())
                .code("HTTP_STATUS_ERROR");

        // Try to convert the exception to get metadata with suggestions
        try {
            BusinessException convertedException = converterService.convertException(ex);
            Map<String, Object> metadata = convertedException.getMetadata();

            // Add metadata to the response
            builder.metadata(metadata);

            // Add suggestion from metadata if available
            if (metadata != null && metadata.containsKey("suggestion")) {
                builder.suggestion(metadata.get("suggestion").toString());
            } else {
                // Add suggestion based on the status code
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    builder.suggestion("The requested resource could not be found. Please check the URL and try again.");
                } else if (ex.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED) {
                    builder.suggestion("The HTTP method used is not allowed for this resource. Please check the documentation for allowed methods.");
                } else if (ex.getStatusCode() == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                    builder.suggestion("The request format is not supported. Please check the Content-Type header.");
                } else if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    builder.suggestion("The request was malformed. Please check your request parameters and try again.");
                } else {
                    builder.suggestion("Please check your request and try again.");
                }
            }
        } catch (Exception conversionError) {
            // If conversion fails, use default suggestions
            log.debug("Error converting ResponseStatusException", conversionError);

            // Add suggestion based on the status code
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                builder.suggestion("The requested resource could not be found. Please check the URL and try again.");
            } else if (ex.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED) {
                builder.suggestion("The HTTP method used is not allowed for this resource. Please check the documentation for allowed methods.");
            } else if (ex.getStatusCode() == HttpStatus.UNSUPPORTED_MEDIA_TYPE) {
                builder.suggestion("The request format is not supported. Please check the Content-Type header.");
            } else if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                builder.suggestion("The request was malformed. Please check your request parameters and try again.");
            } else {
                builder.suggestion("Please check your request and try again.");
            }
        }

        // Add documentation link
        builder.documentation("https://api.example.com/docs/errors/http-status");

        ErrorResponse errorResponse = builder.build();

        exchange.getResponse().setStatusCode(ex.getStatusCode());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(toJson(errorResponse).getBytes())));
    }

    private Mono<Void> handleUnexpectedError(ServerWebExchange exchange, Throwable ex) {
        log.error("Unexpected error occurred: ", ex);

        String traceId = UUID.randomUUID().toString();
        log.error("Trace ID: {}", traceId, ex);

        // Create a more detailed error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please contact support with the trace ID.")
                .path(exchange.getRequest().getPath().value())
                .traceId(traceId)
                .code("INTERNAL_ERROR")
                .suggestion("Please try again later or contact support with the trace ID.")
                .documentation("https://api.example.com/docs/errors/internal-error")
                .details("The server encountered an unexpected condition that prevented it from fulfilling the request.")
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