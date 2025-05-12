# Firefly Common Web Library

A comprehensive library for Spring WebFlux applications in the Firefly platform, providing standardized error handling, OpenAPI documentation, idempotency support, and other essential web-related utilities.

## 📋 Table of Contents

- [✨ Overview](#-overview)
- [🚀 Quick Start](#-quick-start)
- [🔧 Features](#-features)
- [📦 Installation](#-installation)
  - [Requirements](#requirements)
  - [Compatibility](#compatibility)
- [📘 Usage](#-usage)
  - [🌟 Examples and Use Cases](#-examples-and-use-cases)
  - [⚡ Performance Considerations](#-performance-considerations)
  - [Exception Handling](#exception-handling)
  - [OpenAPI Documentation](#openapi-documentation)
  - [Idempotency Support](#idempotency-support)
- [⚙️ Configuration Properties](#️-configuration-properties)
- [🧪 Testing](#-testing)
  - [Testing Best Practices](#testing-best-practices)
- [🏆 Best Practices](#-best-practices)
  - [General Best Practices](#general-best-practices)
  - [Exception Handling Best Practices](#exception-handling-best-practices)
  - [Idempotency Best Practices](#idempotency-best-practices)
- [❓ Troubleshooting](#-troubleshooting)
- [🔄 Version History](#-version-history)
- [🔮 Roadmap](#-roadmap)
- [🔒 Security Considerations](#-security-considerations)
- [👥 Contributing](#-contributing)
- [📄 License](#-license)
- [❓ FAQ](#-faq)
- [🆘 Getting Help](#-getting-help)

## ✨ Overview

The Firefly Common Web Library is designed to standardize and simplify the development of Spring WebFlux applications within the Firefly platform. It provides a comprehensive set of tools and utilities that address common web application requirements, ensuring consistency across services and reducing boilerplate code.

<div align="center">
  <pre>
  ┌─────────────────────────────────────────────────────────────────┐
  │                     Spring WebFlux Application                  │
  │                                                                 │
  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
  │  │   Request   │    │  Firefly    │    │     Business        │  │
  │  │   ───────▶  │───▶│  Common Web │───▶│     Logic           │  │
  │  │             │    │  Library    │    │                     │  │
  │  └─────────────┘    └─────────────┘    └─────────────────────┘  │
  │         │                 │                      │              │
  │         │                 ▼                      │              │
  │         │          ┌─────────────┐               │              │
  │         │          │ Idempotency │               │              │
  │         │          │   Cache     │               │              │
  │         │          └─────────────┘               │              │
  │         │                                        │              │
  │         └────────────────────────────────────────┘              │
  │                              │                                  │
  │                              ▼                                  │
  │                     ┌─────────────────┐                         │
  │                     │    Response     │                         │
  │                     │  Standardization│                         │
  │                     └─────────────────┘                         │
  │                              │                                  │
  │                              ▼                                  │
  │                     ┌─────────────────┐                         │
  │                     │     Client      │                         │
  │                     └─────────────────┘                         │
  └─────────────────────────────────────────────────────────────────┘
  </pre>
</div>

### Key Benefits

- **Standardized Error Handling**: Consistent error responses across all services
- **Automatic Documentation**: OpenAPI/Swagger documentation with minimal configuration
- **Idempotency Support**: Prevent duplicate operations with simple configuration
- **Zero-Configuration Setup**: Works out of the box with Spring Boot's auto-configuration

This library is built with Spring Boot's auto-configuration mechanism, allowing for zero-configuration setup in most cases, while still providing extensive customization options when needed.

## 🚀 Quick Start

1. **Add the dependency**:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **Use the library in your code**:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public Mono<User> getUser(@PathVariable String id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(
                ResourceNotFoundException.forResource("User", id)
            ));
    }

    @PostMapping
    public Mono<User> createUser(@Valid @RequestBody User user) {
        // Idempotency is automatically handled for POST requests
        return userService.createUser(user);
    }
}
```

3. **Real-world example: Payment Processing API**

Here's a more complete example showing how to use the library's features in a payment processing API:

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Process a payment", description = "Processes a payment and returns the payment details")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payment details"),
        @ApiResponse(responseCode = "409", description = "Payment already processed (when same X-Idempotency-Key is reused)")
    })
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey) {

        return paymentService.processPayment(paymentRequest)
            .map(payment -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PaymentResponse.fromPayment(payment)))
            .onErrorResume(PaymentDeclinedException.class, ex ->
                Mono.error(new InvalidRequestException(
                    "PAYMENT_DECLINED",
                    "Payment was declined: " + ex.getMessage()
                ))
            )
            .onErrorResume(PaymentProviderException.class, ex ->
                Mono.error(ServiceException.dependencyFailure(
                    "Payment Provider",
                    ex.getMessage()
                ))
            );
    }

    @GetMapping("/{id}")
    public Mono<PaymentResponse> getPayment(@PathVariable String id) {
        return paymentService.findPayment(id)
            .switchIfEmpty(Mono.error(
                ResourceNotFoundException.forResource("Payment", id)
            ))
            .map(PaymentResponse::fromPayment);
    }
}
```

This example demonstrates:
- OpenAPI documentation with `@Operation` and `@ApiResponse` annotations
- Idempotency support with the required `X-Idempotency-Key` header
- Exception handling with specialized exceptions
- Response entity customization
- Error mapping from service-specific exceptions to standard API exceptions

## 🔧 Features

### 🛡️ Comprehensive Exception Handling

<details>
<summary><b>Click to expand</b></summary>

- **Standardized error responses** for all exception types
- **Rich hierarchy of business exceptions** for common error scenarios:
  - `ResourceNotFoundException` (404)
  - `InvalidRequestException` (400)
  - `ConflictException` (409)
  - `UnauthorizedException` (401)
  - `ForbiddenException` (403)
  - `ServiceException` (500)
  - And many more...
- **Automatic conversion** of standard Java and Spring exceptions
- **Detailed validation error handling** with field-level information
- **Support for error codes, metadata, and documentation links**

</details>

### 📝 OpenAPI Documentation

<details>
<summary><b>Click to expand</b></summary>

- **Automatic configuration** of OpenAPI/Swagger documentation
- **Environment-aware server configurations**:
  - Local: `http://localhost:{port}`
  - Dev: `https://dev-api.getfirefly.io`
  - Staging: `https://staging-api.getfirefly.io`
  - Production: `https://api.getfirefly.io`
- **Customizable API information**, contact details, and license
- **Optional JWT security definitions**

<div align="center">
  <img src="https://raw.githubusercontent.com/swagger-api/swagger-ui/master/docs/images/swagger-ui-screenshot.png" width="700" alt="Swagger UI Screenshot">
  <p><i>Example of the Swagger UI generated for your API</i></p>
</div>

</details>

### 🔄 Idempotency Support

<details>
<summary><b>Click to expand</b></summary>

- **Automatic idempotency handling** for HTTP POST, PUT, and PATCH requests
- **Prevents duplicate operations** with the same idempotency key
- **Configurable caching** with in-memory (Caffeine) or Redis options
- **Selective disabling** for specific endpoints using `@DisableIdempotency`
- **Transparent to clients** - no changes needed in client code beyond adding the header

</details>

### ⚙️ Spring Boot Auto-configuration

<details>
<summary><b>Click to expand</b></summary>

- **Zero-configuration setup** for Spring Boot applications
- **Extensive customization options** via properties
- **Conditional dependencies** for optional features

</details>

## 📦 Installation

### Maven

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

For Gradle projects, add to your `build.gradle`:

```groovy
implementation 'com.catalis:lib-common-web:1.0.0-SNAPSHOT'
```

### Requirements

- Java 17 or higher
- Spring Boot 3.x

### Compatibility

This library is compatible with the following Spring Boot starters and libraries:

| Library | Compatible Versions | Notes |
|---------|---------------------|-------|
| `spring-boot-starter-webflux` | 3.x | Required dependency |
| `spring-boot-starter-data-redis-reactive` | 3.x | Required, for Redis-based idempotency caching |
| `spring-boot-starter-security` | 3.x | Optional, for security integration |
| `spring-boot-starter-validation` | 3.x | Optional, for validation support |
| `springdoc-openapi-starter-webflux-ui` | 2.x | Optional, for OpenAPI documentation |
| `spring-cloud-starter-gateway` | 4.x | Compatible when used in API Gateway projects |
| `spring-boot-starter-data-r2dbc` | 3.x | Compatible with R2DBC exception handling |

The library is designed to work seamlessly with other Spring Boot starters and libraries, with minimal configuration required.

## 📘 Usage

### 🌟 Examples and Use Cases

Here are some common use cases and examples of how to use the library in real-world scenarios:

#### Idempotency Support

The library automatically adds idempotency support to all POST, PUT, and PATCH endpoints. This means:

1. The `X-Idempotency-Key` header is automatically added to the OpenAPI documentation for these endpoints
2. Requests with the same idempotency key will only be processed once
3. Subsequent requests with the same key will receive the cached response

Here's how to disable idempotency for specific endpoints when you don't need it:

```java
@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // This endpoint has automatic idempotency support
    // Clients should include an X-Idempotency-Key header
    @PostMapping
    public Mono<ResponseEntity<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceRequest request) {
        return resourceService.createResource(request)
            .map(resource -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResourceResponse.fromResource(resource)));
    }

    // This endpoint has idempotency disabled
    // The @DisableIdempotency annotation prevents idempotency processing
    @PostMapping("/non-idempotent")
    @DisableIdempotency
    public Mono<ResponseEntity<ResourceResponse>> createNonIdempotentResource(
            @Valid @RequestBody ResourceRequest request) {
        return resourceService.createResource(request)
            .map(resource -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResourceResponse.fromResource(resource)));
    }
}
```

#### Handling Validation Errors

This example shows how to handle validation errors using the library:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Mono<ResponseEntity<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request)
            .map(user -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UserResponse.fromUser(user)))
            .onErrorResume(DataIntegrityViolationException.class, ex -> {
                if (ex.getMessage().contains("email_unique")) {
                    return Mono.error(ConflictException.resourceAlreadyExists("User", request.getEmail()));
                }
                return Mono.error(ex);
            });
    }

    // Custom validator for complex validation logic
    @Component
    public static class UserValidator {
        public Mono<UserRequest> validate(UserRequest request) {
            ValidationException.Builder validationBuilder = new ValidationException.Builder();

            if (request.getPassword() != null && request.getPassword().length() < 8) {
                validationBuilder.addError("password", "must be at least 8 characters");
            }

            if (request.getEmail() != null && !request.getEmail().contains("@")) {
                validationBuilder.addError("email", "must be a valid email address");
            }

            if (validationBuilder.hasErrors()) {
                return Mono.error(validationBuilder.build());
            }

            return Mono.just(request);
        }
    }
}
```

#### Integration with External Services

This example shows how to handle errors when integrating with external services:

```java
@Service
public class ExternalPaymentService {
    private final WebClient webClient;
    private final Logger log = LoggerFactory.getLogger(ExternalPaymentService.class);

    public ExternalPaymentService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl("https://payment-provider-api.example.com")
            .build();
    }

    public Mono<PaymentResult> processExternalPayment(PaymentRequest request) {
        return webClient.post()
            .uri("/api/v1/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResult.class)
            .timeout(Duration.ofSeconds(10))
            .doOnError(WebClientResponseException.class, ex -> {
                log.error("Payment provider returned error: {} - {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            })
            .onErrorResume(WebClientResponseException.NotFound.class, ex ->
                Mono.error(ServiceException.dependencyFailure(
                    "Payment Provider",
                    "The requested resource was not found"
                ))
            )
            .onErrorResume(WebClientResponseException.BadRequest.class, ex ->
                Mono.error(new InvalidRequestException(
                    "INVALID_PAYMENT_REQUEST",
                    "The payment provider rejected the request: " + ex.getResponseBodyAsString()
                ))
            )
            .onErrorResume(TimeoutException.class, ex ->
                Mono.error(ServiceException.dependencyFailure(
                    "Payment Provider",
                    "The request timed out after 10 seconds"
                ))
            );
    }
}
```

### ⚡ Performance Considerations

When using this library, keep the following performance considerations in mind:

#### Idempotency Cache

- **Memory Usage**: The in-memory cache can consume significant memory if `idempotency.cache.max-entries` is set too high
- **Redis Performance**: When using Redis for idempotency caching, ensure your Redis instance is properly sized for your workload
- **TTL Settings**: Set appropriate TTL values based on your business requirements to avoid unnecessary cache growth
- **Response Size**: Large response bodies will consume more memory in the cache

#### Exception Handling

- **Stack Traces**: In production, consider disabling full stack traces in error responses to reduce response size
- **Custom Exception Mapping**: Complex exception mapping logic can impact performance; keep it simple
- **Logging**: Excessive logging in exception handlers can impact performance during error scenarios

#### OpenAPI Documentation

- **Production Environments**: Consider disabling Swagger UI in production environments to reduce memory usage
- **Documentation Generation**: OpenAPI documentation generation adds a small overhead to application startup time

#### General Recommendations

- **Reactive Programming**: Follow reactive programming best practices to avoid blocking operations
- **Connection Pooling**: Configure appropriate connection pool sizes for external service clients
- **Timeouts**: Set appropriate timeouts for all external service calls
- **Monitoring**: Implement monitoring to track performance metrics and identify bottlenecks

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

<table>
<tr>
<th>Exception Type</th>
<th>HTTP Status</th>
<th>Usage Example</th>
</tr>
<tr>
<td><code>ResourceNotFoundException</code></td>
<td>404 Not Found</td>
<td>

```java
// Basic usage
throw new ResourceNotFoundException("User not found");

// With error code
throw new ResourceNotFoundException("RESOURCE_NOT_FOUND", "User not found");

// Using factory method
throw ResourceNotFoundException.forResource("User", "123");
```

</td>
</tr>
<tr>
<td><code>InvalidRequestException</code></td>
<td>400 Bad Request</td>
<td>

```java
// Basic usage
throw new InvalidRequestException("Invalid input parameters");

// Using factory method
throw InvalidRequestException.forField("email", "invalid-email",
    "must be a valid email format");
```

</td>
</tr>
<tr>
<td><code>ConflictException</code></td>
<td>409 Conflict</td>
<td>

```java
// Basic usage
throw new ConflictException("User already exists");

// Using factory method
throw ConflictException.resourceAlreadyExists("User",
    "john.doe@example.com");
```

</td>
</tr>
<tr>
<td><code>UnauthorizedException</code></td>
<td>401 Unauthorized</td>
<td>

```java
// Basic usage
throw new UnauthorizedException("Authentication required");

// Using factory methods
throw UnauthorizedException.missingAuthentication();
throw UnauthorizedException.invalidCredentials();
```

</td>
</tr>
<tr>
<td><code>ForbiddenException</code></td>
<td>403 Forbidden</td>
<td>

```java
// Basic usage
throw new ForbiddenException("Insufficient permissions");

// Using factory methods
throw ForbiddenException.insufficientPermissions("ADMIN");
throw ForbiddenException.resourceAccessDenied("Document", "123");
```

</td>
</tr>
<tr>
<td><code>ServiceException</code></td>
<td>500 Internal Server Error</td>
<td>

```java
// Basic usage
throw new ServiceException("Database connection failed");

// Using factory methods
throw ServiceException.withCause("Failed to process request", exception);
throw ServiceException.dependencyFailure("Payment Service",
    "Timeout after 30s");
```

</td>
</tr>
<tr>
<td><code>ValidationException</code></td>
<td>400 Bad Request</td>
<td>

```java
// With multiple field errors
ValidationException.Builder validationBuilder =
    new ValidationException.Builder()
        .addError("email", "must be a valid email")
        .addError("password", "must be at least 8 characters");
throw validationBuilder.build();
```

</td>
</tr>
</table>

#### Automatic Suggestions and Metadata

The library automatically adds helpful suggestions and relevant metadata to all exceptions through two aspects:

1. **ExceptionMetadataAspect**: Automatically adds metadata to exceptions, such as:
   - Exception type
   - Exception ID (for tracking)
   - Source information (class, method, line)
   - Cause information
   - Exception-specific metadata based on the type of exception

2. **ExceptionSuggestionAspect**: Automatically adds user-friendly suggestions to exceptions based on:
   - HTTP status code
   - Exception category
   - Error code
   - Available metadata

These aspects ensure that all error responses include helpful information for both users and developers without requiring manual addition of suggestions or metadata.

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

| Field | Description | Required |
|-------|-------------|----------|
| `timestamp` | The time when the error occurred | ✅ |
| `path` | The request path that caused the error | ✅ |
| `status` | The HTTP status code | ✅ |
| `error` | The HTTP status description | ✅ |
| `message` | A human-readable error message | ✅ |
| `traceId` | A unique identifier for the error (useful for troubleshooting) | ✅ |
| `code` | A machine-readable error code | ✅ |
| `errors` | A list of field-specific validation errors (only for validation errors) | ❌ |
| `details` | Additional details about the error | ❌ |
| `suggestion` | A suggestion for how to resolve the error | ❌ |
| `documentation` | A link to documentation about the error | ❌ |
| `metadata` | Additional metadata about the error | ❌ |

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

| Java/Spring Exception | Converted To | HTTP Status |
|-----------------------|--------------|-------------|
| `NoSuchElementException` | `ResourceNotFoundException` | 404 |
| `IllegalArgumentException` | `InvalidRequestException` | 400 |
| `DataIntegrityViolationException` | `DataIntegrityException` | 409 |
| `AccessDeniedException` | `ForbiddenException` | 403 |
| `WebExchangeBindException` | `ValidationException` | 400 |
| `WebClientResponseException` | Various based on status | Varies |
| `R2dbcException` | Various based on cause | Varies |

#### Error Codes

The library defines a comprehensive set of error codes for precise error identification. For a complete list of all error codes, see the [ERROR_CODES.md](src/main/java/com/catalis/common/web/error/ERROR_CODES.md) document.

### OpenAPI Documentation

The library automatically configures OpenAPI documentation for your API, making it easy to document your endpoints and provide a user-friendly interface for API exploration.

<div align="center">
  <img src="https://static1.smartbear.co/swagger/media/assets/images/swagger_logo.svg" width="300" alt="Swagger UI">
</div>

#### OpenAPI Configuration

You can customize the OpenAPI documentation using the following properties in your `application.properties` or `application.yml`:

```yaml
# Basic application info
spring:
  application:
    name: My Service
    version: 1.0.0
    description: My Service API Documentation

    # Team contact information
    team:
      name: Development Team
      email: developers@getfirefly.io
      url: https://getfirefly.io

    # License information
    license:
      name: Apache 2.0
      url: https://www.apache.org/licenses/LICENSE-2.0

# OpenAPI configuration
openapi:
  security:
    enabled: true
  servers:
    enabled: true
```

#### Server Environments

The library automatically configures server URLs based on the active Spring profiles:

| Profile | Server URL |
|---------|------------|
| `default`, `local` | `http://localhost:{serverPort}{contextPath}` |
| `dev` | `https://dev-api.getfirefly.io{contextPath}` |
| `staging` | `https://staging-api.getfirefly.io{contextPath}` |
| `prod` | `https://api.getfirefly.io{contextPath}` |

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

#### Accessing the Documentation

Once your application is running, you can access the OpenAPI documentation at:

- **Swagger UI**: `http://localhost:{port}/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:{port}/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:{port}/v3/api-docs.yaml`

### Idempotency Support

The library provides automatic idempotency handling for HTTP POST, PUT, and PATCH requests. This ensures that repeated requests with the same idempotency key will only be processed once, preventing duplicate operations.

#### How It Works

1. Client includes an `X-Idempotency-Key` header with a unique value (e.g., UUID) in their request
2. If this is the first request with this key, it's processed normally and the response is cached
3. If the same key is used again, the cached response is returned without processing the request again
4. Keys automatically expire after a configurable time period
5. The `X-Idempotency-Key` header is automatically added to the OpenAPI documentation for all POST, PUT, and PATCH endpoints

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

> **Note**: The Redis dependency (`spring-boot-starter-data-redis-reactive`) is included by default. When Redis is properly configured and `idempotency.cache.redis.enabled=true`, the library will use Redis for idempotency caching. If Redis is not configured or not accessible, the library will automatically fall back to using the in-memory cache.

## ⚙️ Configuration Properties

The library uses Spring Boot's auto-configuration mechanism, so it works out of the box with no additional configuration required. However, you can customize its behavior using the following properties:

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `spring.application.name` | String | Application name used in OpenAPI documentation | "Service" |
| `spring.application.version` | String | Application version used in OpenAPI documentation | "1.0.0" |
| `spring.application.description` | String | Application description used in OpenAPI documentation | "${spring.application.name} API Documentation" |
| `spring.application.team.name` | String | Team name used in OpenAPI documentation contact information | "Development Team" |
| `spring.application.team.email` | String | Team email used in OpenAPI documentation contact information | "developers@getfirefly.io" |
| `spring.application.team.url` | String | Team URL used in OpenAPI documentation contact information | "https://getfirefly.io" |
| `spring.application.license.name` | String | License name used in OpenAPI documentation | "Apache 2.0" |
| `spring.application.license.url` | String | License URL used in OpenAPI documentation | "https://www.apache.org/licenses/LICENSE-2.0" |
| `openapi.security.enabled` | Boolean | Whether to enable security definitions in OpenAPI documentation | false |
| `openapi.servers.enabled` | Boolean | Whether to enable server definitions in OpenAPI documentation | true |
| `idempotency.cache.redis.enabled` | Boolean | Whether to use Redis for idempotency caching | false |
| `idempotency.cache.ttl-hours` | Integer | Time-to-live in hours for cached responses | 24 |
| `idempotency.cache.max-entries` | Integer | Maximum number of entries in the in-memory cache | 10000 |

## 🧪 Testing

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

### Testing Best Practices

#### Testing Exception Handling

```java
@WebFluxTest(controllers = UserController.class)
class UserControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getUserNotFound_shouldReturnNotFoundWithStandardErrorFormat() {
        // Given
        String userId = "non-existent-id";
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When/Then
        webTestClient.get()
            .uri("/api/users/{id}", userId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.error").isEqualTo("Not Found")
            .jsonPath("$.message").exists()
            .jsonPath("$.path").isEqualTo("/api/users/" + userId)
            .jsonPath("$.timestamp").exists()
            .jsonPath("$.traceId").exists();
    }
}
```

#### Testing Idempotency

```java
@SpringBootTest
@AutoConfigureWebTestClient
class IdempotencyIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void idempotentRequest_shouldReturnSameResponseForSameKey() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = new PaymentRequest(100.0, "USD");

        // When - First request
        PaymentResponse firstResponse = webTestClient.post()
            .uri("/api/payments")
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        // Then - Second request with same key should return identical response
        webTestClient.post()
            .uri("/api/payments")
            .header("Idempotency-Key", idempotencyKey)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .json(objectMapper.writeValueAsString(firstResponse));
    }
}
```

## 🏆 Best Practices

### General Best Practices

1. **Use Specialized Exceptions**: Instead of generic exceptions, use the specialized exceptions provided by the library to ensure consistent error handling.

2. **Provide Meaningful Error Messages**: Always include clear, user-friendly error messages that explain what went wrong and how to fix it.

3. **Include Error Codes**: Use error codes for all business exceptions to make it easier to document and reference errors.

4. **Document API Endpoints**: Use OpenAPI annotations to document all API endpoints, including possible error responses.

5. **Enable Idempotency for State-Changing Operations**: Always enable idempotency for operations that change state, especially payment processing and other financial transactions.

### Exception Handling Best Practices

1. **Map Domain Exceptions**: Create a mapping layer that converts your domain-specific exceptions to the standard exceptions provided by the library.

   ```java
   @Component
   public class DomainExceptionMapper {
       public BusinessException mapToBusinessException(DomainException ex) {
           if (ex instanceof EntityNotFoundException) {
               return ResourceNotFoundException.forResource(
                   ((EntityNotFoundException) ex).getEntityType(),
                   ((EntityNotFoundException) ex).getEntityId()
               );
           }
           // Map other domain exceptions...
           return new ServiceException(ex.getMessage());
       }
   }
   ```

2. **Use Factory Methods**: Use the factory methods provided by exception classes instead of constructors for more readable code.

   ```java
   // Instead of this:
   throw new ResourceNotFoundException("User with ID " + id + " not found");

   // Use this:
   throw ResourceNotFoundException.forResource("User", id);
   ```

3. **Add Context to Exceptions**: Include relevant context in exceptions to make debugging easier.

   ```java
   throw InvalidRequestException.builder()
       .message("Invalid payment request")
       .addError("amount", "must be greater than zero")
       .addError("currency", "must be a valid ISO currency code")
       .build();
   ```

### Idempotency Best Practices

1. **Use UUIDs for Idempotency Keys**: Generate and use UUIDs for idempotency keys to ensure uniqueness.

   ```java
   String idempotencyKey = UUID.randomUUID().toString();
   ```

2. **Store Idempotency Keys Client-Side**: For retries, store the idempotency key client-side and reuse it for retries of the same operation.

3. **Use Redis in Production**: For production environments with multiple service instances, use Redis for idempotency caching to ensure consistency across instances.

4. **Set Appropriate TTL**: Configure an appropriate time-to-live for idempotency keys based on your business requirements.

5. **Include Idempotency Key in Logs**: Log the idempotency key with each request to make it easier to trace requests across systems.

   ```java
   log.info("Processing payment request with idempotency key: {}", idempotencyKey);
   ```

## ❓ Troubleshooting

### Common Issues

#### Redis Connection Issues

If you're using Redis for idempotency caching and encounter connection issues:

1. Verify Redis is running and accessible from your application
2. Check the Redis connection properties in your `application.yml`
3. If Redis is temporarily unavailable, the library will automatically fall back to in-memory caching

**Example error log:**
```
org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**Solution:**
```yaml
# application.yml
spring:
  redis:
    host: your-redis-host
    port: 6379
    password: your-redis-password  # if required
    timeout: 2000ms
```

#### OpenAPI Documentation Not Showing

If the OpenAPI documentation is not available:

1. Ensure `springdoc-openapi-starter-webflux-ui` is on your classpath
2. Check that you're accessing the correct URL (`/swagger-ui.html`)
3. Verify that no custom security configuration is blocking access to the Swagger UI endpoints

**Example error:**
```
404 Not Found: /swagger-ui.html
```

**Solution:**
Check your security configuration to ensure it allows access to Swagger UI paths:

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        // ... other security config
        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        // ... more config
        .build();
}
```

#### Idempotency Not Working

If idempotency handling is not working as expected:

1. Ensure the `Idempotency-Key` header is included in your requests
2. Check that the endpoint is using a supported HTTP method (POST, PUT, PATCH)
3. Verify that the endpoint is not annotated with `@DisableIdempotency`
4. Check the cache configuration in your `application.yml`

**Debugging steps:**

Enable debug logging for the idempotency filter:

```yaml
# application.yml
logging:
  level:
    com.catalis.common.web.idempotency: DEBUG
```

This will show detailed logs about idempotency processing:

```
DEBUG c.c.c.w.i.f.IdempotencyWebFilter - IdempotencyWebFilter.filter: Found Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000
DEBUG c.c.c.w.i.f.IdempotencyWebFilter - IdempotencyWebFilter.filter: Checking cache for key: 123e4567-e89b-12d3-a456-426614174000
```

#### Exception Handling Not Working

If your exceptions are not being handled correctly:

1. Ensure your exceptions extend `BusinessException` or one of its subclasses
2. Check that you're not catching and handling exceptions manually in a way that bypasses the global handler
3. Verify that you haven't registered a custom `WebExceptionHandler` with higher precedence

**Example issue:**
Custom exceptions not being converted to the standard error response format.

**Solution:**
Make sure your custom exceptions extend the appropriate base class:

```java
public class PaymentProcessingException extends ServiceException {
    public PaymentProcessingException(String message) {
        super("PAYMENT_PROCESSING_ERROR", message);
    }
}
```

## 🔄 Version History

### 1.0.0-SNAPSHOT (Current)

- Initial release with core features:
  - Comprehensive exception handling
  - OpenAPI documentation
  - Idempotency support
  - Spring Boot auto-configuration

## 🔮 Roadmap

The following features are planned for upcoming releases:

### Short-term (Next Release)

- OAuth2 security integration
- Rate limiting support
- Circuit breaker integration
- Metrics and monitoring

### Medium-term (Next 3-6 Months)

- GraphQL support
- WebSocket support
- Enhanced logging and tracing
- Distributed tracing integration (OpenTelemetry)
- Enhanced caching mechanisms

### Long-term (6+ Months)

- Reactive database integration helpers
- Event-driven architecture support
- Advanced security features (MFA, API key management)
- AI-assisted error resolution
- Performance optimization tools

We welcome contributions and suggestions for the roadmap. Please see the [Contributing](#-contributing) section for more information on how to get involved.

## 🔒 Security Considerations

### Authentication and Authorization

While this library provides exception handling for security-related errors, it does not implement authentication or authorization directly. We recommend:

- Using Spring Security with JWT or OAuth2 for authentication
- Implementing proper authorization checks in your controllers or service layer
- Following the principle of least privilege for all API endpoints

### Idempotency Key Security

When using idempotency keys:

- Treat idempotency keys as sensitive data
- Use strong, randomly generated UUIDs for idempotency keys
- Do not log the full idempotency key in production environments
- Consider implementing rate limiting to prevent abuse of the idempotency mechanism

### Data Protection

- Do not include sensitive data in error messages or logs
- Be careful about what information is exposed in API responses
- Consider encrypting sensitive data in the idempotency cache

### Dependency Security

- Regularly update dependencies to address security vulnerabilities
- Use dependency scanning tools to identify security issues
- Follow Spring Security advisories for the latest security information

### Production Deployment

- Use HTTPS for all API endpoints
- Configure appropriate CORS settings
- Implement rate limiting to prevent abuse
- Use a Web Application Firewall (WAF) for additional protection

## 👥 Contributing

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

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## ❓ FAQ

### General Questions

#### What is the minimum Spring Boot version required?
This library requires Spring Boot 3.x or higher. It is not compatible with Spring Boot 2.x due to significant changes in the Spring WebFlux API.

#### Can I use this library with Spring MVC instead of WebFlux?
No, this library is specifically designed for reactive applications using Spring WebFlux. For Spring MVC applications, consider using Spring Boot's built-in error handling and documentation features.

#### Is this library compatible with GraalVM native images?
Yes, this library is compatible with GraalVM native images. All components are designed to work with Spring's AOT (Ahead-of-Time) compilation.

### Exception Handling

#### How do I create custom exception types?
You can create custom exception types by extending `BusinessException` or any of its subclasses:

```java
public class PaymentFailedException extends ServiceException {
    public PaymentFailedException(String message) {
        super(HttpStatus.FAILED_DEPENDENCY, "PAYMENT_FAILED", message);
    }

    public static PaymentFailedException withProvider(String provider) {
        return new PaymentFailedException("Payment failed with provider: " + provider);
    }
}
```

#### Can I customize the error response format?
Yes, you can customize the error response format by providing your own implementation of `GlobalErrorWebExceptionHandler`. However, it's recommended to use the default implementation to maintain consistency across services.

### Idempotency

#### How long are idempotency keys stored?
By default, idempotency keys are stored for 24 hours. You can customize this using the `idempotency.cache.ttl-hours` property.

#### What happens if Redis is unavailable when using Redis for idempotency caching?
If Redis is unavailable, the library will automatically fall back to using the in-memory cache. This ensures that idempotency handling continues to work even if Redis is temporarily unavailable.

#### Can I use a custom cache implementation for idempotency?
Yes, you can provide your own implementation of the `IdempotencyCache` interface and register it as a Spring bean. The library will automatically use your custom implementation instead of the default ones.

## 🆘 Getting Help

### Community Support

If you have questions or need help with the library, there are several ways to get support:

1. **GitHub Issues**: For bug reports and feature requests, please [open an issue](https://github.com/getfirefly/lib-common-web/issues) on GitHub.

3. **Internal Documentation**: Comprehensive internal documentation is available on the [Firefly Software Solutions Inc Confluence](https://getfirefly.atlassian.net/wiki/spaces/FIR/pages/123456789/Firefly+Common+Web+Library).

### Commercial Support

Firefly Software Solutions Inc offers commercial support for this library as part of our enterprise support packages. Contact your account manager or email [developers@getfirefly.io](mailto:developers@getfirefly.io) for more information.

### Reporting Security Vulnerabilities

If you discover a security vulnerability, please do NOT open an issue. Email [developers@getfirefly.io](mailto:developers@getfirefly.io) instead.

### Training and Workshops

Firefly Software Solutions Inc offers training sessions and workshops on using the Firefly platform libraries effectively. Contact [developers@getfirefly.io](mailto:developers@getfirefly.io) to schedule a session for your team.
