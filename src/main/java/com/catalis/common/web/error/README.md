# Exception Handling in lib-common-web

A comprehensive exception handling system for Spring WebFlux applications that provides standardized error responses, automatic exception conversion, and a rich set of business exceptions.

> **Note**: For a complete list of all error codes, see the [ERROR_CODES.md](ERROR_CODES.md) document.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Exception Types](#exception-types)
  - [Basic Exceptions](#basic-exceptions)
  - [Additional Exceptions](#additional-exceptions)
  - [HTTP Method Exceptions](#http-method-exceptions)
  - [Gateway and Service Exceptions](#gateway-and-service-exceptions)
- [Usage Guide](#usage-guide)
  - [Throwing Exceptions](#throwing-exceptions)
  - [Exception Conversion](#exception-conversion)
    - [Automatic Conversion](#automatic-conversion)
    - [Manual Conversion](#manual-conversion)
  - [Adding Metadata to Exceptions](#adding-metadata-to-exceptions)
  - [Handling Validation Errors](#handling-validation-errors)
- [Exception Converters](#exception-converters)
  - [Spring Exception Converters](#spring-exception-converters)
  - [Database Exception Converters](#database-exception-converters)
  - [Network Exception Converters](#network-exception-converters)
  - [Other Exception Converters](#other-exception-converters)
- [Creating Custom Exception Converters](#creating-custom-exception-converters)
- [Error Response Format](#error-response-format)
- [Configuration](#configuration)
- [Best Practices](#best-practices)
- [Examples](#examples)
- [Error Codes Reference](#error-codes-reference)

## Overview

The exception handling system in lib-common-web provides a standardized way to handle errors in Spring WebFlux applications. It offers:

1. **Consistent Error Responses**: All errors are transformed into a standardized JSON format with detailed information.
2. **Rich Exception Hierarchy**: A comprehensive set of business exceptions that map to appropriate HTTP status codes.
3. **Automatic Exception Conversion**: Standard Java and Spring exceptions are automatically converted to business exceptions.
4. **Metadata Support**: Exceptions can carry additional metadata to provide context for error handling.
5. **Validation Error Handling**: Special support for handling validation errors with field-level details.

This system ensures that all errors in your application are handled consistently, providing clear and informative error messages to clients while maintaining detailed information for debugging.

## Features

- **Standardized Error Responses**: All errors return a consistent JSON structure
- **HTTP Status Code Mapping**: Exceptions automatically map to appropriate HTTP status codes
- **Error Code System**: Each exception type has specific error codes for precise error identification
- **Detailed Error Messages**: Human-readable error messages with context information
- **Validation Error Support**: Special handling for field validation errors
- **Exception Conversion**: Automatic conversion of standard exceptions to business exceptions
- **Metadata Support**: Ability to attach additional data to exceptions
- **Tracing Support**: Integration with distributed tracing via trace IDs
- **Documentation Links**: Error responses can include links to documentation
- **Extensible Architecture**: Easy to add custom exception types and converters
- **Spring Boot Auto-Configuration**: Automatic setup with Spring Boot applications

## Exception Types

The library provides a rich hierarchy of business exceptions to represent different error scenarios.

### Basic Exceptions

- `BusinessException`: Base exception for all business exceptions
  - HTTP Status: Configurable (default: 400 BAD REQUEST)
  - Use Case: Base class for all other exceptions

- `ResourceNotFoundException`: For resources that cannot be found
  - HTTP Status: 404 NOT FOUND
  - Use Case: When a requested entity doesn't exist
  - Example: `throw ResourceNotFoundException.forResource("User", "123")`

- `InvalidRequestException`: For invalid request parameters
  - HTTP Status: 400 BAD REQUEST
  - Use Case: When request parameters fail validation
  - Example: `throw InvalidRequestException.forField("email", "invalid-email", "must be a valid email format")`

- `ConflictException`: For resource conflicts
  - HTTP Status: 409 CONFLICT
  - Use Case: When an operation conflicts with the current state
  - Example: `throw ConflictException.resourceAlreadyExists("User", "john.doe@example.com")`

- `UnauthorizedException`: For authentication errors
  - HTTP Status: 401 UNAUTHORIZED
  - Use Case: When authentication is required or credentials are invalid
  - Example: `throw UnauthorizedException.missingAuthentication()`

- `ForbiddenException`: For permission errors
  - HTTP Status: 403 FORBIDDEN
  - Use Case: When the user doesn't have sufficient permissions
  - Example: `throw ForbiddenException.insufficientPermissions("ADMIN")`

- `ServiceException`: For internal service errors
  - HTTP Status: 500 INTERNAL SERVER ERROR
  - Use Case: When an unexpected error occurs in the service
  - Example: `throw ServiceException.withCause("Failed to process request", exception)`

- `ValidationException`: For validation errors with multiple fields
  - HTTP Status: 400 BAD REQUEST
  - Use Case: When multiple fields fail validation
  - Example: See [Handling Validation Errors](#handling-validation-errors)

### Additional Exceptions

- `ThirdPartyServiceException`: For errors in external services
  - HTTP Status: 502 BAD GATEWAY
  - Use Case: When an external service returns an error
  - Example: `throw ThirdPartyServiceException.serviceUnavailable("PaymentService")`

- `RateLimitException`: For rate limiting
  - HTTP Status: 429 TOO MANY REQUESTS
  - Use Case: When a client exceeds the allowed request rate
  - Example: `throw RateLimitException.forUser("user123", 60)`

- `DataIntegrityException`: For data integrity issues
  - HTTP Status: 400 BAD REQUEST
  - Use Case: When a database constraint is violated
  - Example: `throw DataIntegrityException.uniqueConstraintViolation("email", "john.doe@example.com")`

- `OperationTimeoutException`: For operation timeouts
  - HTTP Status: 408 REQUEST TIMEOUT
  - Use Case: When an operation takes too long to complete
  - Example: `throw OperationTimeoutException.databaseTimeout("query", 5000)`

- `ConcurrencyException`: For concurrency issues
  - HTTP Status: 409 CONFLICT
  - Use Case: When concurrent modifications conflict
  - Example: `throw ConcurrencyException.optimisticLockingFailure("User", "123")`

- `AuthorizationException`: For authorization issues
  - HTTP Status: 403 FORBIDDEN
  - Use Case: When a user lacks specific permissions
  - Example: `throw AuthorizationException.missingPermission("USER_WRITE")`

### HTTP Method Exceptions

- `MethodNotAllowedException`: For unsupported HTTP methods
  - HTTP Status: 405 METHOD NOT ALLOWED
  - Use Case: When a resource doesn't support the requested HTTP method
  - Example: `throw MethodNotAllowedException.forResource(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT)`

- `UnsupportedMediaTypeException`: For unsupported content types
  - HTTP Status: 415 UNSUPPORTED MEDIA TYPE
  - Use Case: When the request format is not supported
  - Example: `throw UnsupportedMediaTypeException.forMediaType("application/xml", "application/json")`

- `PayloadTooLargeException`: For oversized requests
  - HTTP Status: 413 PAYLOAD TOO LARGE
  - Use Case: When a request body exceeds size limits
  - Example: `throw PayloadTooLargeException.forSize(1048576, 2097152)`

- `PreconditionFailedException`: For failed preconditions
  - HTTP Status: 412 PRECONDITION FAILED
  - Use Case: When request preconditions (like If-Match) fail
  - Example: `throw PreconditionFailedException.forIfMatch("\"abc123\"", "\"xyz789\"")`

- `LockedResourceException`: For locked resources
  - HTTP Status: 423 LOCKED
  - Use Case: When a resource is locked for editing
  - Example: `throw LockedResourceException.forResource("Document", "123", "user456")`

### Gateway and Service Exceptions

- `BadGatewayException`: For gateway communication errors
  - HTTP Status: 502 BAD GATEWAY
  - Use Case: When communication with an external service fails
  - Example: `throw BadGatewayException.forServer("PaymentService", "https://api.payment.com", "INVALID_RESPONSE")`

- `GatewayTimeoutException`: For gateway timeouts
  - HTTP Status: 504 GATEWAY TIMEOUT
  - Use Case: When an external service takes too long to respond
  - Example: `throw GatewayTimeoutException.forServer("PaymentService", "https://api.payment.com", 30000)`

- `ServiceUnavailableException`: For temporarily unavailable services
  - HTTP Status: 503 SERVICE UNAVAILABLE
  - Use Case: When a service is temporarily down
  - Example: `throw ServiceUnavailableException.forServiceWithRetry("InventoryService", 60)`

- `NotImplementedException`: For unimplemented features
  - HTTP Status: 501 NOT IMPLEMENTED
  - Use Case: When a requested feature is not implemented
  - Example: `throw NotImplementedException.forFeature("Export to PDF")`

- `GoneException`: For permanently unavailable resources
  - HTTP Status: 410 GONE
  - Use Case: When a resource is no longer available
  - Example: `throw GoneException.forResourceWithDetails("API", "v1", "2023-01-01", "Deprecated version")`

## Usage Guide

### Throwing Exceptions

You can throw business exceptions directly in your code. Each exception type provides factory methods to create instances with appropriate error messages and metadata.

```java
// Resource not found (404)
throw ResourceNotFoundException.forResource("User", "123");

// Invalid request (400)
throw InvalidRequestException.forField("email", "invalid-email", "must be a valid email format");

// Conflict (409)
throw ConflictException.resourceAlreadyExists("User", "john.doe@example.com");

// Unauthorized (401)
throw UnauthorizedException.missingAuthentication();

// Forbidden (403)
throw ForbiddenException.insufficientPermissions("ADMIN");

// Service error (500)
throw ServiceException.withCause("Failed to process request", exception);
```

You can also create exceptions using constructors:

```java
// Basic exception with default status (400 Bad Request)
throw new BusinessException("Invalid input provided");

// Exception with specific HTTP status
throw new BusinessException(HttpStatus.CONFLICT, "Resource already exists");

// Exception with error code
throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input provided");
```

### Exception Conversion

The library provides a mechanism to convert standard exceptions to business exceptions.

#### Automatic Conversion

By default, all exceptions handled by the `GlobalExceptionHandler` are automatically converted to appropriate business exceptions. This means you don't need to catch and convert standard exceptions in most cases.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        // If findById throws any exception, it will be automatically
        // converted to an appropriate BusinessException
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new NoSuchElementException("User not found")));
    }
}
```

The `GlobalExceptionHandler` handles the conversion process:

1. If the exception is already a `BusinessException`, it's used directly
2. If it's a `ValidationException` or `WebExchangeBindException`, it's handled by specialized handlers
3. For all other exceptions, the `ExceptionConverterService` tries to convert them to a `BusinessException`
4. If conversion fails, the exception is handled as an unexpected error

#### Manual Conversion

In some cases, you may want to manually convert exceptions, for example, to add custom metadata or to handle exceptions in a specific way.

```java
@Service
public class UserService {
    private final ExceptionConverterService converterService;

    public UserService(ExceptionConverterService converterService) {
        this.converterService = converterService;
    }

    public Mono<User> createUser(User user) {
        return Mono.fromCallable(() -> {
            try {
                return userRepository.save(user);
            } catch (Exception e) {
                // Convert the exception to a business exception
                BusinessException businessException = converterService.convertException(e);
                
                // Add custom metadata
                businessException = businessException.withMetadata("userId", user.getId());
                
                throw businessException;
            }
        });
    }
}
```

### Adding Metadata to Exceptions

You can add metadata to exceptions to provide additional context for error handling. The metadata will be included in the error response.

```java
// Create an exception
BusinessException exception = new InvalidRequestException("Invalid request parameters");

// Add metadata
exception = exception.withMetadata("requestId", "123456");
exception = exception.withMetadata("timestamp", System.currentTimeMillis());

// Add multiple metadata entries at once
Map<String, Object> metadata = new HashMap<>();
metadata.put("clientIp", "192.168.1.1");
metadata.put("userAgent", "Mozilla/5.0");
exception = exception.withMetadata(metadata);

throw exception;
```

### Handling Validation Errors

For validation errors with multiple fields, use the `ValidationException.Builder`:

```java
// Create a validation exception builder
ValidationException.Builder validationBuilder = new ValidationException.Builder();

// Add validation errors
validationBuilder.addError("email", "must be a valid email");
validationBuilder.addError("password", "must be at least 8 characters");

// Add error with code and metadata
Map<String, Object> metadata = new HashMap<>();
metadata.put("minLength", 8);
metadata.put("maxLength", 20);
validationBuilder.addError("username", "INVALID_LENGTH", "must be between 8 and 20 characters", metadata);

// Build and throw the exception
throw validationBuilder.build();
```

The builder also supports nested validation errors:

```java
// Add nested validation errors
validationBuilder.addNestedError("address", "city", "required field");
validationBuilder.addNestedError("address", "zipCode", "INVALID_FORMAT", "must be a valid zip code");

// Add all errors from another validation exception
ValidationException otherException = /* ... */;
validationBuilder.addErrors(otherException);

// Add all errors from another validation exception with a parent field prefix
validationBuilder.addNestedErrors("billingAddress", otherException);
```

## Exception Converters

The library provides converters for standard exceptions to automatically convert them to business exceptions.

### Spring Exception Converters

- `DataAccessExceptionConverter`: Converts Spring data access exceptions
  - Handles: `DataAccessException` and its subclasses
  - Example: Converts `DataIntegrityViolationException` to `DataIntegrityException`

- `HttpClientErrorExceptionConverter`: Converts Spring HTTP client exceptions
  - Handles: `HttpClientErrorException` and its subclasses
  - Example: Converts `HttpClientErrorException.NotFound` to `ResourceNotFoundException`

- `HttpServerErrorExceptionConverter`: Converts Spring HTTP server exceptions
  - Handles: `HttpServerErrorException` and its subclasses
  - Example: Converts `HttpServerErrorException.ServiceUnavailable` to `ServiceUnavailableException`

- `OptimisticLockingFailureExceptionConverter`: Converts Spring optimistic locking exceptions
  - Handles: `OptimisticLockingFailureException`
  - Example: Converts to `ConcurrencyException`

### Database Exception Converters

- `JpaExceptionConverter`: Converts JPA exceptions
  - Handles: `EntityNotFoundException`, `PersistenceException`
  - Example: Converts `EntityNotFoundException` to `ResourceNotFoundException`

- `R2dbcExceptionConverter`: Converts R2DBC exceptions
  - Handles: `R2dbcException` and its subclasses
  - Example: Converts `R2dbcDataIntegrityViolationException` to `DataIntegrityException`

### Network Exception Converters

- `NetworkExceptionConverter`: Converts network exceptions
  - Handles: `ConnectException`, `SocketTimeoutException`, `UnknownHostException`
  - Example: Converts `SocketTimeoutException` to `OperationTimeoutException`

### Other Exception Converters

- `ValidationExceptionConverter`: Converts validation exceptions
  - Handles: `MethodArgumentNotValidException`, `BindException`, `WebExchangeBindException`
  - Example: Converts to `ValidationException`

- `SecurityExceptionConverter`: Converts security exceptions
  - Handles: `AccessDeniedException`, `AuthenticationException`
  - Example: Converts `AccessDeniedException` to `ForbiddenException`

- `JsonExceptionConverter`: Converts JSON processing exceptions
  - Handles: `JsonProcessingException`
  - Example: Converts to `InvalidRequestException`

- `WebFluxExceptionConverter`: Converts WebFlux-specific exceptions
  - Handles: Various WebFlux exceptions
  - Example: Converts to appropriate business exceptions

- `ExternalServiceExceptionConverter`: Converts exceptions from external service calls
  - Handles: Various exceptions from external service calls
  - Example: Converts to appropriate gateway exceptions

## Creating Custom Exception Converters

You can create custom exception converters to handle specific exceptions in your application.

1. Implement the `ExceptionConverter` interface:

```java
@Component
public class MyExceptionConverter implements ExceptionConverter<MyException> {

    @Override
    public Class<MyException> getExceptionType() {
        return MyException.class;
    }

    @Override
    public BusinessException convert(MyException exception) {
        return new BusinessException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "MY_ERROR",
                "Custom error: " + exception.getMessage()
        );
    }
}
```

2. The converter will be automatically registered with the `ExceptionConverterService` and used to convert exceptions of the specified type.

## Error Response Format

All exceptions are converted to a standardized JSON response format:

```json
{
  "timestamp": "01/01/2023T12:34:56.789000",
  "path": "/api/users/123",
  "status": 404,
  "error": "Not Found",
  "message": "User with id '123' not found",
  "traceId": "abc123def456",
  "code": "RESOURCE_NOT_FOUND",
  "details": "The requested resource does not exist or has been removed",
  "suggestion": "Check the resource identifier and try again",
  "documentation": "https://api.example.com/docs/errors/resource_not_found",
  "metadata": {
    "resourceType": "User",
    "resourceId": "123"
  },
  "errors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "must be a valid email format",
      "metadata": {
        "pattern": "^[A-Za-z0-9+_.-]+@(.+)$"
      }
    }
  ]
}
```

Fields:
- `timestamp`: When the error occurred
- `path`: The request path
- `status`: The HTTP status code
- `error`: The HTTP status reason
- `message`: A human-readable error message
- `traceId`: A unique identifier for the request (for tracing)
- `code`: A machine-readable error code
- `details`: Additional details about the error
- `suggestion`: A suggestion for how to resolve the error
- `documentation`: A link to documentation about the error
- `metadata`: Additional metadata about the error
- `errors`: A list of field-level validation errors (for validation exceptions)

## Configuration

The library is automatically configured when included in a Spring Boot project through the `ExceptionHandlerConfiguration` class, which uses Spring's auto-configuration mechanism.

```java
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = {
        "com.catalis.common.web.error.handler",
        "com.catalis.common.web.error.converter"
})
public class ExceptionHandlerConfiguration {
}
```

No additional configuration is required. The auto-configuration is activated through the `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file.

## Best Practices

1. **Use Factory Methods**: Use the provided factory methods to create exceptions with appropriate error messages and metadata.

2. **Be Specific**: Use the most specific exception type for each error scenario.

3. **Include Context**: Provide enough context in error messages to help users understand and fix the issue.

4. **Add Metadata**: Use metadata to provide additional context for error handling and debugging.

5. **Document Error Codes**: Document all error codes used in your application in the [ERROR_CODES.md](ERROR_CODES.md) file.

6. **Consistent Error Handling**: Handle errors consistently across your application.

7. **Validation**: Use `ValidationException` for field-level validation errors.

8. **Security**: Don't expose sensitive information in error messages.

9. **Testing**: Write tests for error scenarios to ensure proper error handling.

10. **Leverage Automatic Conversion**: Let the `GlobalExceptionHandler` handle exception conversion when possible.

## Examples

For complete examples of how to use these features, see the `ExceptionHandlingExample` and `ExceptionHandlingController` classes included in the library.

### Basic Example

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return userService.getUser(id)
            .switchIfEmpty(Mono.error(ResourceNotFoundException.forResource("User", id)));
    }

    @PostMapping
    public Mono<User> createUser(@Valid @RequestBody User user) {
        return userService.createUser(user)
            .onErrorMap(DataIntegrityViolationException.class, e -> {
                if (e.getMessage().contains("email_unique")) {
                    return ConflictException.resourceAlreadyExists("User", user.getEmail());
                }
                return DataIntegrityException.withReason("DATA_ERROR", e.getMessage());
            });
    }
}
```

### Validation Example

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        // Manual validation
        ValidationException.Builder validationBuilder = new ValidationException.Builder();
        
        if (user.getEmail() == null || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            validationBuilder.addError("email", "INVALID_FORMAT", "must be a valid email format");
        }
        
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            validationBuilder.addError("password", "TOO_SHORT", "must be at least 8 characters");
        }
        
        if (validationBuilder.hasErrors()) {
            return Mono.error(validationBuilder.build());
        }
        
        return userService.createUser(user);
    }
}
```

### Automatic Exception Conversion Example

```java
@RestController
@RequestMapping("/api/examples/exceptions")
public class ExceptionHandlingController {

    @GetMapping("/auto-convert/{type}")
    public Mono<String> throwStandardException(@PathVariable String type) {
        return Mono.defer(() -> {
            try {
                // This will throw a standard exception
                if ("timeout".equals(type)) {
                    throw new TimeoutException("Operation timed out after 5000ms");
                } else if ("data-integrity".equals(type)) {
                    throw new DataIntegrityViolationException("Duplicate entry");
                } else {
                    throw new RuntimeException("Unknown exception: " + type);
                }
            } catch (TimeoutException e) {
                // The GlobalExceptionHandler will automatically convert this to a BusinessException
                throw new RuntimeException("Timeout occurred", e);
            }
        });
    }
}
```

## Error Codes Reference

For a complete list of all error codes used in the library, see the [ERROR_CODES.md](ERROR_CODES.md) document. This document contains all error codes organized by exception type and HTTP code.
