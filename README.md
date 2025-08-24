# Firefly Common Web Library

A production-ready Spring Boot starter library for reactive web applications, providing standardized exception handling, request idempotency, OpenAPI documentation, and comprehensive web utilities for Spring WebFlux applications. This is the **Common Library for Web Modules** within the **Firefly OpenCore Banking Platform**, developed by the Firefly Team.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Features](#core-features)
  - [Exception Handling](#exception-handling)
  - [Request Idempotency](#request-idempotency)
  - [OpenAPI Integration](#openapi-integration)
  - [HTTP Request Logging](#http-request-logging)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Overview

The Firefly Common Web Library is a comprehensive Spring Boot starter designed for reactive web applications. It provides essential web utilities that standardize common patterns across microservices, focusing on reliability, observability, and developer productivity.

### Key Benefits

- **Zero Configuration**: Auto-configured with sensible defaults
- **Production Ready**: Battle-tested patterns for enterprise applications
- **Reactive First**: Built specifically for Spring WebFlux applications
- **Extensible**: Easily customizable and extensible architecture
- **Type Safe**: Comprehensive exception hierarchy with detailed error handling

## Features

### 🛡️ Exception Handling
- **24 specialized exception types** with appropriate HTTP status code mapping
- **Global exception handler** with automatic conversion from standard exceptions
- **Standardized error responses** with trace IDs, suggestions, and metadata
- **Validation support** with detailed field-level error information
- **Built-in error suggestions** and documentation links

### 🔄 Request Idempotency
- **Automatic idempotency** for POST, PUT, and PATCH requests
- **Dual cache support**: In-memory (Caffeine) and distributed (Redis)
- **Configurable TTL** and cache size limits
- **Selective disabling** via `@DisableIdempotency` annotation
- **Response caching and replay** functionality

### 📝 OpenAPI Integration
- **Auto-configuration** with environment-aware settings
- **Security scheme integration** for API documentation
- **Idempotency header documentation** for applicable operations
- **Customizable API metadata** and contact information

### 📊 HTTP Request Logging
- **Structured logging** with request/response correlation
- **Configurable filtering** and content size limits
- **Sensitive data protection** with automatic masking
- **Performance metrics** with request duration tracking

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Prerequisites

- Java 17 or higher
- Spring Boot 3.x
- Spring WebFlux (reactive web stack)

### Required Dependencies

The library automatically provides these key dependencies:
- `spring-boot-starter-webflux`
- `spring-boot-starter-actuator`
- `spring-boot-starter-validation`
- `springdoc-openapi-starter-webflux-ui`
- `caffeine` (for in-memory caching)
- `spring-boot-starter-data-redis-reactive` (for distributed caching)

## Quick Start

1. **Add the dependency** to your project
2. **Enable auto-configuration** - The library automatically configures itself when present on the classpath (no additional annotations required beyond `@SpringBootApplication`)
3. **Configure properties** (optional) in `application.yml` - The library works with sensible defaults

```yaml
# Optional configuration - defaults are provided
idempotency:
  header-name: X-Idempotency-Key  # Default header name
  cache:
    ttl-hours: 24  # Cache TTL in hours
    max-entries: 10000  # Max cache entries (in-memory)
    redis:
      enabled: false  # Use Redis cache instead of in-memory

# Note: HTTP request logging is handled by Spring Boot's built-in logging
logging:
  level:
    com.catalis.common.web: DEBUG  # Enable detailed logging
```

4. **Start using the features**:

```java
@RestController
public class MyController {
    
    @PostMapping("/api/data")
    public Mono<ResponseEntity<String>> createData(@RequestBody String data) {
        // Idempotency is automatically applied to POST requests
        return Mono.just(ResponseEntity.ok("Data created"));
    }
    
    @PostMapping("/api/special")
    @DisableIdempotency  // Disable idempotency for specific endpoints
    public Mono<ResponseEntity<String>> specialEndpoint(@RequestBody String data) {
        return Mono.just(ResponseEntity.ok("Special operation"));
    }
}
```

## Core Features

### Exception Handling

The library provides a comprehensive exception handling system with 24 specialized exception types:

#### Available Exception Types

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ValidationException` | 400 | Request validation failures |
| `InvalidRequestException` | 400 | Invalid request format or parameters |
| `UnauthorizedException` | 401 | Authentication required |
| `ForbiddenException` | 403 | Insufficient permissions |
| `ResourceNotFoundException` | 404 | Resource not found |
| `MethodNotAllowedException` | 405 | HTTP method not allowed |
| `ConflictException` | 409 | Resource conflict |
| `GoneException` | 410 | Resource no longer available |
| `PreconditionFailedException` | 412 | Precondition not met |
| `PayloadTooLargeException` | 413 | Request payload too large |
| `UnsupportedMediaTypeException` | 415 | Unsupported media type |
| `LockedResourceException` | 423 | Resource is locked |
| `RateLimitException` | 429 | Rate limit exceeded |
| `ServiceException` | 500 | Internal service error |
| `NotImplementedException` | 501 | Feature not implemented |
| `BadGatewayException` | 502 | Bad gateway response |
| `ServiceUnavailableException` | 503 | Service temporarily unavailable |
| `GatewayTimeoutException` | 504 | Gateway timeout |
| `BusinessException` | Configurable | Base class for custom business exceptions |
| `ConcurrencyException` | 409 | Concurrent modification conflicts |
| `DataIntegrityException` | 422 | Data integrity violations |
| `ThirdPartyServiceException` | 502 | Third-party service errors |
| `OperationTimeoutException` | 408 | Operation timeout |
| `AuthorizationException` | 403 | Authorization failures |

#### Usage Example

```java
@Service
public class UserService {
    
    public Mono<User> findUserById(String userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                "User not found", 
                "USER_NOT_FOUND", 
                Map.of("userId", userId)
            )));
    }
    
    public Mono<User> createUser(CreateUserRequest request) {
        return validateUser(request)
            .flatMap(userRepository::save)
            .onErrorMap(DataIntegrityViolationException.class, 
                ex -> new ConflictException("User already exists", "USER_EXISTS"));
    }
}
```

#### Error Response Format

All exceptions produce standardized error responses:

```json
{
  "timestamp": "2024-08-24T23:20:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/users/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "code": "USER_NOT_FOUND",
  "suggestion": "Verify the user ID and ensure it exists.",
  "metadata": {
    "userId": "123"
  }
}
```

### Request Idempotency

Automatic idempotency support for POST, PUT, and PATCH requests using the `X-Idempotency-Key` header.

#### Configuration Options

```yaml
idempotency:
  header-name: X-Idempotency-Key  # HTTP header name (default)
  cache:
    ttl-hours: 1  # Cache TTL in hours (default: 24)
    max-entries: 1000  # Max cache entries for in-memory cache (default: 10000)
    redis:
      enabled: false  # Set to true to use Redis instead of in-memory cache
```

#### Cache Implementations

##### In-Memory Cache (Caffeine)
- Default option for single-instance deployments
- Configurable maximum size and TTL
- High performance with automatic eviction

##### Redis Cache
- For distributed deployments
- Shared across multiple application instances
- Requires Redis connection configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # Additional Redis configuration
```

#### Disabling Idempotency

Use the `@DisableIdempotency` annotation to disable idempotency for specific endpoints:

```java
@RestController
public class FileController {
    
    @PostMapping("/api/files/upload")
    @DisableIdempotency  // File uploads should not be idempotent
    public Mono<ResponseEntity<String>> uploadFile(@RequestBody MultiValueMap<String, Part> parts) {
        return fileService.uploadFile(parts)
            .map(result -> ResponseEntity.ok(result));
    }
}
```

### OpenAPI Integration

Automatic OpenAPI documentation generation with idempotency header integration.

#### Features
- Automatic server URL configuration based on environment
- Security scheme integration
- Idempotency header documentation for applicable operations
- Customizable API metadata

#### Configuration

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

### HTTP Request Logging

Structured logging for HTTP requests and responses with correlation support.

#### Features
- Automatic request/response correlation with trace IDs
- Configurable content size limits
- Sensitive data masking
- JSON structured logging format
- Performance metrics tracking

#### Configuration

```yaml
# Configure logging levels for detailed request/response logging
logging:
  level:
    com.catalis.common.web.logging: DEBUG  # Enable HTTP request logging
    org.springframework.web.reactive: DEBUG  # Enable WebFlux logging
    reactor.netty.http.server: DEBUG  # Enable Netty HTTP server logging
```

## Configuration

### Complete Configuration Reference

```yaml
# Idempotency configuration (main feature of this library)
idempotency:
  header-name: X-Idempotency-Key  # HTTP header name for idempotency key
  cache:
    ttl-hours: 24  # Time-to-live in hours for cached responses
    max-entries: 10000  # Maximum entries in in-memory cache
    redis:
      enabled: false  # Set to true to use Redis instead of in-memory cache

# Redis configuration (when using Redis cache)
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: # Optional
      timeout: 2000ms
      database: 0

# OpenAPI/Swagger configuration
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html

# Logging configuration for request/response logging
logging:
  level:
    com.catalis.common.web: INFO  # Set to DEBUG for detailed request logging
    org.springframework.web.reactive: DEBUG  # Enable WebFlux request logging
```

## Usage Examples

### Custom Exception Handling

```java
@RestController
public class OrderController {
    
    @PostMapping("/api/orders")
    public Mono<ResponseEntity<Order>> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(request)
            .map(order -> ResponseEntity.status(HttpStatus.CREATED).body(order))
            .onErrorMap(InsufficientFundsException.class, 
                ex -> new BusinessException("Insufficient funds for order", 
                    HttpStatus.PAYMENT_REQUIRED, 
                    "INSUFFICIENT_FUNDS",
                    Map.of("orderId", request.getId(), "amount", request.getAmount())))
            .onErrorMap(ProductNotFoundException.class,
                ex -> new ResourceNotFoundException("Product not found", 
                    "PRODUCT_NOT_FOUND",
                    Map.of("productId", ex.getProductId())));
    }
}
```

### Custom Idempotency Configuration

```java
@Configuration
public class IdempotencyConfig {
    
    @Bean
    @ConditionalOnProperty(value = "catalis.common.web.idempotency.cache-type", havingValue = "redis")
    public IdempotencyCache redisIdempotencyCache(ReactiveRedisTemplate<String, CachedResponse> redisTemplate,
                                                  IdempotencyProperties properties) {
        return new RedisIdempotencyCache(redisTemplate, properties);
    }
}
```

### Request Logging Customization

```java
@Component
public class CustomRequestLoggingFilter implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomRequestLoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        exchange.getRequest().mutate()
            .headers(headers -> headers.add("X-Trace-ID", traceId));
        
        logger.info("Request started: {} {} [{}]", 
            exchange.getRequest().getMethod(),
            exchange.getRequest().getPath().value(),
            traceId);
        
        return chain.filter(exchange)
            .doFinally(signalType -> 
                logger.info("Request completed: {} [{}]", signalType, traceId));
    }
}
```

## Testing

The library includes comprehensive test utilities and examples:

### Test Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Example Test

```java
@WebFluxTest
@Import(GlobalExceptionHandler.class)
class ApiControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldReturnStandardErrorResponse() {
        webTestClient.get()
            .uri("/api/nonexistent")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.error").isEqualTo("Not Found")
            .jsonPath("$.traceId").exists()
            .jsonPath("$.timestamp").exists();
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotencyIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldReturnCachedResponseForDuplicateRequest() {
        String idempotencyKey = UUID.randomUUID().toString();
        
        // First request
        webTestClient.post()
            .uri("/api/data")
            .header("X-Idempotency-Key", idempotencyKey)
            .bodyValue("{\"data\": \"test\"}")
            .exchange()
            .expectStatus().isOk();
        
        // Second request with same idempotency key
        webTestClient.post()
            .uri("/api/data")
            .header("X-Idempotency-Key", idempotencyKey)
            .bodyValue("{\"data\": \"different\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .json("{\"message\": \"Data created\"}"); // Same response as first request
    }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Boot and reactive programming best practices
- Write comprehensive tests for new features
- Update documentation for API changes
- Ensure backward compatibility
- Follow the existing code style and patterns

### Building the Project

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Generate documentation
mvn javadoc:javadoc

# Create JAR
mvn clean package
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

*Part of the **Firefly OpenCore Banking Platform** - Common Library for Web Modules*