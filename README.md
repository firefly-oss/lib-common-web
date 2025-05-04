# lib-common-web

A common library for Spring WebFlux web applications in the Firefly platform, providing standardized error handling, OpenAPI documentation, and other web-related utilities.

## Features

- **Global Exception Handling**: Standardized error responses for various exception types
- **Business Exceptions**: Custom exception hierarchy for common error scenarios
- **Validation Support**: Detailed validation error handling
- **OpenAPI Documentation**: Automatic configuration of OpenAPI/Swagger documentation with environment-aware server configurations
- **Security Definitions**: Optional JWT security definitions for OpenAPI
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

Common exception types are also available with enhanced functionality:

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
  ]
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
spring.application.team.url=https://catalis.com

# License information
spring.application.license.name=Apache 2.0
spring.application.license.url=https://www.apache.org/licenses/LICENSE-2.0

# OpenAPI configuration
openapi.security.enabled=true
openapi.servers.enabled=true
```

The library automatically configures server URLs based on the active Spring profiles:

- `default`, `local`, `dev`: Adds `http://localhost:{serverPort}{contextPath}`
- `dev`: Adds `https://dev-api.catalis.com{contextPath}`
- `staging`: Adds `https://staging-api.catalis.com{contextPath}`
- `prod`: Adds `https://api.catalis.com{contextPath}`

When security is enabled, the library adds a JWT Bearer Token security scheme to the OpenAPI documentation.

## Configuration

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

## Testing

The library includes comprehensive unit tests for all functionality. You can run the tests using Maven:

```bash
mvn test
```

The test suite includes:

- Tests for all exception types and their factory methods
- Tests for the global exception handler
- Tests for the OpenAPI configuration

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request