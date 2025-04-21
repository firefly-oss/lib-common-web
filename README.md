# lib-common-web

A common library for Spring WebFlux web applications in the Firefly platform, providing standardized error handling, OpenAPI documentation, and other web-related utilities.

## Features

- **Global Exception Handling**: Standardized error responses for various exception types
- **Business Exceptions**: Custom exception hierarchy for common error scenarios
- **OpenAPI Documentation**: Automatic configuration of OpenAPI/Swagger documentation
- **Spring Boot Auto-configuration**: Zero-configuration setup for Spring Boot applications

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Exception Handling

The library provides a standardized way to handle exceptions in your application:

```java
// Throw a business exception
throw new BusinessException("Invalid input provided");

// Throw a business exception with custom HTTP status
throw new BusinessException(HttpStatus.CONFLICT, "Resource already exists");

// Throw a business exception with error code
throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input provided");
```

Common exception types are also available:

```java
// Resource not found (404)
throw new ResourceNotFoundException("User not found");

// Invalid request (400)
throw new InvalidRequestException("Invalid input parameters");

// Conflict (409)
throw new ConflictException("User already exists");

// Unauthorized (401)
throw new UnauthorizedException("Authentication required");

// Forbidden (403)
throw new ForbiddenException("Insufficient permissions");
```

### Error Response Format

All exceptions are converted to a standardized JSON response format:

```json
{
  "timestamp": "01/01/2023T12:00:00.000000",
  "path": "/api/resource",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input provided",
  "traceId": "abc123",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email"
    }
  ]
}
```

### OpenAPI Documentation

The library automatically configures OpenAPI documentation for your API. You can customize it using the following properties in your `application.properties` or `application.yml`:

```properties
# Basic application info
spring.application.name=My Service
spring.application.version=1.0.0
spring.application.description=My Service API Documentation

# Team contact information
spring.application.team.name=Development Team
spring.application.team.email=team@catalis.com
```

## Configuration

The library uses Spring Boot's auto-configuration mechanism, so it works out of the box with no additional configuration required. However, you can customize its behavior using the following properties:

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `spring.application.name` | String | Application name used in OpenAPI documentation | "Service" |
| `spring.application.version` | String | Application version used in OpenAPI documentation | "1.0.0" |
| `spring.application.description` | String | Application description used in OpenAPI documentation | "${spring.application.name} API Documentation" |
| `spring.application.team.name` | String | Team name used in OpenAPI documentation contact information | "Development Team" |
| `spring.application.team.email` | String | Team email used in OpenAPI documentation contact information | "team@catalis.com" |

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.