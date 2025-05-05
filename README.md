# Firefly Common Web Library

A comprehensive library for Spring WebFlux applications in the Firefly platform, providing standardized error handling, OpenAPI documentation, idempotency support, and other essential web-related utilities.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
  - [Exception Handling](#exception-handling)
    - [Basic Exception Usage](#basic-exception-usage)
    - [Specialized Exceptions](#specialized-exceptions)
    - [Error Response Format](#error-response-format)
    - [Exception Conversion](#exception-conversion)
    - [Error Codes](#error-codes)
  - [OpenAPI Documentation](#openapi-documentation)
    - [Configuration](#openapi-configuration)
    - [Server Environments](#server-environments)
    - [Security Definitions](#security-definitions)
  - [Idempotency Support](#idempotency-support)
    - [How It Works](#how-it-works)
    - [Disabling Idempotency](#disabling-idempotency-for-specific-endpoints)
    - [Client Usage](#client-usage)
    - [Caching Configuration](#caching-configuration)
- [Configuration Properties](#configuration-properties)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Firefly Common Web Library is designed to standardize and simplify the development of Spring WebFlux applications within the Firefly platform. It provides a comprehensive set of tools and utilities that address common web application requirements, ensuring consistency across services and reducing boilerplate code.

This library is built with Spring Boot's auto-configuration mechanism, allowing for zero-configuration setup in most cases, while still providing extensive customization options when needed.

## Features

- **Comprehensive Exception Handling**
  - Standardized error responses for all exception types
  - Rich hierarchy of business exceptions for common error scenarios
  - Automatic conversion of standard Java and Spring exceptions
  - Detailed validation error handling with field-level information
  - Support for error codes, metadata, and documentation links

- **OpenAPI Documentation**
  - Automatic configuration of OpenAPI/Swagger documentation
  - Environment-aware server configurations
  - Customizable API information, contact details, and license
  - Optional JWT security definitions

- **Idempotency Support**
  - Automatic idempotency handling for HTTP POST, PUT, and PATCH requests
  - Prevents duplicate operations with the same idempotency key
  - Configurable caching with in-memory (Caffeine) or Redis options
  - Selective disabling for specific endpoints

- **Spring Boot Auto-configuration**
  - Zero-configuration setup for Spring Boot applications
  - Extensive customization options via properties
  - Conditional dependencies for optional features

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For Gradle projects, add to your `build.gradle`:

```groovy
implementation 'com.catalis:lib-common-web:1.0.0-SNAPSHOT'
```

## Usage

### Exception Handling

The library provides a comprehensive exception handling system that ensures all errors in your application are handled consistently, providing clear and informative error messages to clients while maintaining detailed information for debugging.

#### Basic Exception Usage

```java
// Throw a basic business exception
throw new BusinessException("Invalid input provided");

// Throw a business exception with custom HTTP status
throw new BusinessException(HttpStatus.CONFLICT, "Resource already exists");

// Throw a business exception with error code
throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input provided");
```

#### Specialized Exceptions

The library includes specialized exceptions for common error scenarios:

```java
// Resource not found (404)
throw new ResourceNotFoundException("User not found");
// With error code
throw new ResourceNotFoundException("RESOURCE_NOT_FOUND", "User not found");
// Using factory method
throw ResourceNotFoundException.forResource("User", "123");

// Invalid request (400)
throw new InvalidRequestException("Invalid input parameters");
// Using factory method
throw InvalidRequestException.forField("email", "invalid-email", "must be a valid email format");

// Conflict (409)
throw new ConflictException("User already exists");
// Using factory method
throw ConflictException.resourceAlreadyExists("User", "john.doe@example.com");

// Unauthorized (401)
throw new UnauthorizedException("Authentication required");
// Using factory methods
throw UnauthorizedException.missingAuthentication();
throw UnauthorizedException.invalidCredentials();

// Forbidden (403)
throw new ForbiddenException("Insufficient permissions");
// Using factory methods
throw ForbiddenException.insufficientPermissions("ADMIN");
throw ForbiddenException.resourceAccessDenied("Document", "123");

// Service error (500)
throw new ServiceException("Database connection failed");
// Using factory methods
throw ServiceException.withCause("Failed to process request", exception);
throw ServiceException.dependencyFailure("Payment Service", "Timeout after 30s");

// Validation error (400) with multiple field errors
ValidationException.Builder validationBuilder = new ValidationException.Builder()
    .addError("email", "must be a valid email")
    .addError("password", "must be at least 8 characters");
throw validationBuilder.build();
```

#### Error Response Format

All exceptions are converted to a standardized JSON response format:

```json
{
  "timestamp": "01/01/2023T12:00:00.000000",
  "path": "/api/resource",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input provided",
  "traceId": "abc123",
  "code": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email"
    },
    {
      "field": "password",
      "message": "must be at least 8 characters"
    }
  ],
  "details": "Additional details about the error",
  "suggestion": "Try using a valid email format",
  "documentation": "https://docs.example.com/errors/VALIDATION_ERROR",
  "metadata": {
    "requestId": "req-123",
    "additionalInfo": "Some additional context"
  }
}
```

The error response includes:

- `timestamp`: The time when the error occurred
- `path`: The request path that caused the error
- `status`: The HTTP status code
- `error`: The HTTP status description
- `message`: A human-readable error message
- `traceId`: A unique identifier for the error (useful for troubleshooting)
- `code`: A machine-readable error code
- `errors`: A list of field-specific validation errors (only for validation errors)
- `details`: Additional details about the error (optional)
- `suggestion`: A suggestion for how to resolve the error (optional)
- `documentation`: A link to documentation about the error (optional)
- `metadata`: Additional metadata about the error (optional)

#### Exception Conversion

The library automatically converts standard Java and Spring exceptions to appropriate business exceptions:

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

Common conversions include:

- `NoSuchElementException` → `ResourceNotFoundException`
- `IllegalArgumentException` → `InvalidRequestException`
- `DataIntegrityViolationException` → `DataIntegrityException`
- `AccessDeniedException` → `ForbiddenException`
- `WebExchangeBindException` → `ValidationException`

#### Error Codes

The library defines a comprehensive set of error codes for precise error identification. For a complete list of all error codes, see the [ERROR_CODES.md](src/main/java/com/catalis/common/web/error/ERROR_CODES.md) document.

### OpenAPI Documentation

The library automatically configures OpenAPI documentation for your API, making it easy to document your endpoints and provide a user-friendly interface for API exploration.

#### OpenAPI Configuration

You can customize the OpenAPI documentation using the following properties in your `application.properties` or `application.yml`:

```properties
# Basic application info
spring.application.name=My Service
spring.application.version=1.0.0
spring.application.description=My Service API Documentation

# Team contact information
spring.application.team.name=Development Team
spring.application.team.email=team@catalis.com
spring.application.team.url=https://catalis.com

# License information
spring.application.license.name=Apache 2.0
spring.application.license.url=https://www.apache.org/licenses/LICENSE-2.0

# OpenAPI configuration
openapi.security.enabled=true
openapi.servers.enabled=true
```

#### Server Environments

The library automatically configures server URLs based on the active Spring profiles:

- `default`, `local`, `dev`: Adds `http://localhost:{serverPort}{contextPath}`
- `dev`: Adds `https://dev-api.catalis.com{contextPath}`
- `staging`: Adds `https://staging-api.catalis.com{contextPath}`
- `prod`: Adds `https://api.catalis.com{contextPath}`

This ensures that your OpenAPI documentation always points to the correct server URL for the current environment.

#### Security Definitions

When security is enabled (`openapi.security.enabled=true`), the library adds a JWT Bearer Token security scheme to the OpenAPI documentation:

```json
"securitySchemes": {
  "bearer-jwt": {
    "type": "http",
    "scheme": "bearer",
    "bearerFormat": "JWT",
    "description": "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\""
  }
}
```

This security scheme is applied globally to all operations, making it clear that authentication is required for API access.

### Idempotency Support

The library provides automatic idempotency handling for HTTP POST, PUT, and PATCH requests. This ensures that repeated requests with the same idempotency key will only be processed once, preventing duplicate operations.

#### How It Works

1. Client includes an `Idempotency-Key` header with a unique value (e.g., UUID) in their request
2. If this is the first request with this key, it's processed normally and the response is cached
3. If the same key is used again, the cached response is returned without processing the request again
4. Keys automatically expire after a configurable time period
5. The `Idempotency-Key` header is automatically added to the OpenAPI documentation for all POST, PUT, and PATCH endpoints

#### Disabling Idempotency for Specific Endpoints

You can disable idempotency for specific endpoints using the `@DisableIdempotency` annotation:

```java
@PostMapping("/api/non-idempotent-operation")
@DisableIdempotency
public Mono<ResponseEntity<Resource>> createResource(@RequestBody Resource resource) {
    // Method implementation
}
```

When this annotation is applied:
- The idempotency filter will not process requests to this endpoint
- The `Idempotency-Key` header will not be included in the OpenAPI documentation for this operation

#### Client Usage

Clients should include an `Idempotency-Key` header with a unique value in their requests:

```http
POST /api/payments HTTP/1.1
Content-Type: application/json
Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000

{
  "amount": 100.00,
  "currency": "USD",
  "description": "Payment for order #12345"
}
```

If this request is sent multiple times with the same idempotency key, the payment will only be processed once.

#### Caching Configuration

By default, idempotency uses an in-memory cache (Caffeine). For distributed environments, you can enable Redis for shared caching:

```yaml
# application.yml
idempotency:
  cache:
    redis:
      enabled: false  # Set to true to use Redis instead of in-memory cache
    ttl-hours: 24     # Time-to-live for cached responses
    max-entries: 10000  # Maximum cache entries (only for in-memory cache)

# Redis configuration (when redis.enabled=true)
spring:
  redis:
    host: localhost
    port: 6379
```

> **Note**: The Redis dependency (`spring-boot-starter-data-redis-reactive`) is optional. If you want to use Redis for idempotency caching (`idempotency.cache.redis.enabled=true`), you need to include this dependency in your project and make sure Redis is properly configured and accessible. If Redis is not configured or the dependency is not included, the library will automatically fall back to using the in-memory cache.

## Configuration Properties

The library uses Spring Boot's auto-configuration mechanism, so it works out of the box with no additional configuration required. However, you can customize its behavior using the following properties:

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `spring.application.name` | String | Application name used in OpenAPI documentation | "Service" |
| `spring.application.version` | String | Application version used in OpenAPI documentation | "1.0.0" |
| `spring.application.description` | String | Application description used in OpenAPI documentation | "${spring.application.name} API Documentation" |
| `spring.application.team.name` | String | Team name used in OpenAPI documentation contact information | "Development Team" |
| `spring.application.team.email` | String | Team email used in OpenAPI documentation contact information | "team@catalis.com" |
| `spring.application.team.url` | String | Team URL used in OpenAPI documentation contact information | "https://catalis.com" |
| `spring.application.license.name` | String | License name used in OpenAPI documentation | "Apache 2.0" |
| `spring.application.license.url` | String | License URL used in OpenAPI documentation | "https://www.apache.org/licenses/LICENSE-2.0" |
| `openapi.security.enabled` | Boolean | Whether to enable security definitions in OpenAPI documentation | false |
| `openapi.servers.enabled` | Boolean | Whether to enable server definitions in OpenAPI documentation | true |
| `idempotency.cache.redis.enabled` | Boolean | Whether to use Redis for idempotency caching | false |
| `idempotency.cache.ttl-hours` | Integer | Time-to-live in hours for cached responses | 24 |
| `idempotency.cache.max-entries` | Integer | Maximum number of entries in the in-memory cache | 10000 |

## Testing

The library includes comprehensive unit tests for all functionality. You can run the tests using Maven:

```bash
mvn test
```

The test suite includes:

- Tests for all exception types and their factory methods
- Tests for the global exception handler
- Tests for the OpenAPI configuration
- Tests for the idempotency module (both in-memory and Redis modes)

When implementing your own services using this library, it's recommended to write tests that verify the correct handling of exceptions and idempotency in your application.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

When contributing, please:

- Follow the existing code style
- Write tests for new features
- Update documentation as needed
- Add entries to the [ERROR_CODES.md](src/main/java/com/catalis/common/web/error/ERROR_CODES.md) file for any new error codes

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
