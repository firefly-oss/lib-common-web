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
  - [PII Data Masking](#pii-data-masking)
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

### 🔒 PII Data Masking
- **Automatic PII detection** using configurable regex patterns
- **39+ built-in patterns** covering emails, phone numbers (US + 26 European countries), SSNs, credit cards, IP/MAC addresses, JWT tokens, API keys, and national identity cards (15+ European countries)
- **Custom pattern support** for organization-specific sensitive data
- **Multiple masking strategies** (preserve length, partial reveal, custom mask characters)
- **Automatic logging protection** - automatically masks PII in ALL application logs and stdout
- **Integration with logging** to protect sensitive data in logs and exception messages

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.firefly</groupId>
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
    com.firefly.common.web: DEBUG  # Enable detailed logging
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
- **No additional dependencies required** (included by default)

**Dependencies:**
```xml
<!-- Already included in lib-common-web -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

##### Redis Cache
- For distributed deployments
- Shared across multiple application instances
- Requires Redis connection configuration
- **No additional dependencies required** (included by default)

**Dependencies:**
```xml
<!-- Already included in lib-common-web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

##### Hazelcast Cache
- For distributed deployments with clustering support
- Shared across multiple application instances
- Provides high availability and fault tolerance
- **Requires additional dependencies**

**Dependencies:**

*Maven:*
```xml
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-spring</artifactId>
    <version>5.3.6</version>
</dependency>
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast</artifactId>
    <version>5.3.6</version>
</dependency>
```

*Gradle:*
```gradle
implementation 'com.hazelcast:hazelcast-spring:5.3.6'
implementation 'com.hazelcast:hazelcast:5.3.6'
```

##### EhCache
- For local deployments with disk persistence
- Single-instance deployments that require cache persistence across restarts
- Better performance than distributed caches for local scenarios
- **Requires additional dependencies**

**Dependencies:**

*Maven:*
```xml
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
    <version>3.10.8</version>
</dependency>
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

*Gradle:*
```gradle
implementation 'org.ehcache:ehcache:3.10.8'
implementation 'javax.cache:cache-api:1.1.1'
```

**Configuration:**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: # Optional
      timeout: 2000ms
      database: 0
      # Additional Redis configuration
```

##### Hazelcast Configuration

**Configuration:**
```yaml
idempotency:
  cache:
    hazelcast:
      enabled: true  # Enable Hazelcast cache
      map-name: idempotencyCache  # Hazelcast IMap name (optional, default: idempotencyCache)
```

**Implementation Example:**

You need to provide Hazelcast function beans in your application configuration:

```java
@Configuration
public class HazelcastIdempotencyConfiguration {

    @Autowired
    private HazelcastInstance hazelcastInstance;
    
    @Autowired
    private IdempotencyProperties properties;

    @Bean
    public Function<String, Mono<CachedResponse>> hazelcastIdempotencyGetFunction() {
        return key -> {
            String mapName = properties.getCache().getHazelcast().getMapName();
            IMap<String, CachedResponse> map = hazelcastInstance.getMap(mapName);
            return Mono.fromSupplier(() -> map.get(key))
                    .filter(Objects::nonNull);
        };
    }

    @Bean
    public BiFunction<String, CachedResponse, Mono<Boolean>> hazelcastIdempotencySetFunction() {
        return (key, response) -> {
            String mapName = properties.getCache().getHazelcast().getMapName();
            IMap<String, CachedResponse> map = hazelcastInstance.getMap(mapName);
            Duration ttl = Duration.ofHours(properties.getCache().getTtlHours());
            return Mono.fromSupplier(() -> {
                map.put(key, response, ttl.toMillis(), TimeUnit.MILLISECONDS);
                return true;
            });
        };
    }
}
```

##### EhCache Configuration

**Configuration:**
```yaml
idempotency:
  cache:
    ehcache:
      enabled: true  # Enable EhCache
      cache-name: idempotencyCache  # EhCache cache name (optional, default: idempotencyCache)
      disk-persistent: true  # Enable disk persistence (optional, default: true)
```

**Implementation Example:**

You need to provide EhCache function beans in your application configuration:

```java
@Configuration
public class EhCacheIdempotencyConfiguration {

    @Autowired
    private IdempotencyProperties properties;

    @Bean
    public CacheManager ehCacheManager() {
        CachingProvider provider = Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
        CacheManager cacheManager = provider.getCacheManager();
        
        // Configure cache programmatically
        Configuration<String, CachedResponse> config = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class, 
                CachedResponse.class,
                ResourcePoolsBuilder.heap(properties.getCache().getMaxEntries())
                    .disk(1, MemoryUnit.GB, properties.getCache().getEhcache().isDiskPersistent())
            )
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
                Duration.ofHours(properties.getCache().getTtlHours())
            ))
            .build()
        );
        
        String cacheName = properties.getCache().getEhcache().getCacheName();
        cacheManager.createCache(cacheName, config);
        return cacheManager;
    }

    @Bean
    public Function<String, Mono<CachedResponse>> ehcacheIdempotencyGetFunction(CacheManager cacheManager) {
        return key -> {
            String cacheName = properties.getCache().getEhcache().getCacheName();
            Cache<String, CachedResponse> cache = cacheManager.getCache(cacheName, String.class, CachedResponse.class);
            return Mono.fromSupplier(() -> cache.get(key))
                    .filter(Objects::nonNull);
        };
    }

    @Bean
    public BiFunction<String, CachedResponse, Mono<Void>> ehcacheIdempotencyPutFunction(CacheManager cacheManager) {
        return (key, response) -> {
            String cacheName = properties.getCache().getEhcache().getCacheName();
            Cache<String, CachedResponse> cache = cacheManager.getCache(cacheName, String.class, CachedResponse.class);
            return Mono.fromRunnable(() -> cache.put(key, response));
        };
    }
}
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
- **Comprehensive PII data masking** (see [PII Data Masking](#pii-data-masking) section below)
- JSON structured logging format
- Performance metrics tracking

#### Configuration

```yaml
# Configure logging levels for detailed request/response logging
logging:
  level:
    com.firefly.common.web.logging: DEBUG  # Enable HTTP request logging
    org.springframework.web.reactive: DEBUG  # Enable WebFlux logging
    reactor.netty.http.server: DEBUG  # Enable Netty HTTP server logging
```

### PII Data Masking

Advanced PII (Personally Identifiable Information) masking system that automatically detects and masks sensitive data in logs, exception messages, request/response bodies, headers, and query parameters.

#### Features
- **Automatic PII detection** using configurable regex patterns
- **Automatic logging protection** - automatically intercepts and masks PII in ALL application logs and stdout
- **Built-in patterns** for common PII types: emails, phone numbers, SSNs, credit cards, IP addresses, JWT tokens, API keys
- **European identity cards support**: DNI, NIE (Spain), and other national identity formats
- **Custom pattern support** for organization-specific sensitive data  
- **Multiple masking strategies**: preserve length, partial reveal, custom mask characters
- **Comprehensive integration**: logs, exception messages, request/response bodies, headers, query parameters
- **Performance optimized**: pre-compiled regex patterns with thread-safe caching
- **Graceful error handling**: continues operation even with invalid patterns
- **Zero-configuration setup**: works automatically when the library is included

#### Built-in PII Patterns

The library includes **39+ pre-built patterns** covering a comprehensive range of sensitive data types:

##### Core Patterns
| Pattern Name | Pattern Type | Example | Masked Result |
|--------------|--------------|---------|---------------|
| `email` | Email addresses | `user@example.com` | `*****************` |
| `phone-us` | US Phone numbers | `555-123-4567` | `*************` |
| `ssn` | US Social Security | `123-45-6789` | `***********` |
| `credit-card` | Credit card numbers | `4532-1234-5678-9012` | `*******************` |
| `ip-address` | IP addresses | `192.168.1.1` | `************` |
| `mac-address` | MAC addresses | `00:1B:44:11:3A:B7` | `*****************` |
| `jwt` | JWT tokens | `eyJhbGci...` | `*********` |
| `api-key` | API keys | `api_key=abc123...` | `api_key=******` |
| `sensitive-url` | URLs with secrets | `https://api.com?token=abc` | `*********************` |

##### European Phone Numbers (26+ Countries)
| Pattern Name | Country | Mobile Format | Landline Format |
|--------------|---------|---------------|-----------------|
| `phone-spain` | Spain | `+34 6XX XXX XXX` | `+34 9XX XXX XXX` |
| `phone-france` | France | `+33 6XX XX XX XX` | `+33 [1-5]XX XX XX XX` |
| `phone-germany` | Germany | `+49 1XX XXX XXXX` | `+49 XXX XXXXXXX` |
| `phone-italy` | Italy | `+39 3XX XXX XXXX` | `+39 0X XXX XXXX` |
| `phone-uk` | United Kingdom | `+44 7XXX XXXXXX` | `+44 1XXX XXXXXX` |
| `phone-netherlands` | Netherlands | `+31 6 XXXX XXXX` | `+31 XX XXX XXXX` |
| `phone-belgium` | Belgium | `+32 4XX XX XX XX` | `+32 X XXX XX XX` |
| `phone-portugal` | Portugal | `+351 9X XXX XXXX` | `+351 2XX XXX XXX` |
| `phone-austria` | Austria | `+43 6XX XXX XXX` | `+43 1 XXX XXXX` |
| `phone-switzerland` | Switzerland | `+41 7X XXX XX XX` | `+41 XX XXX XX XX` |
| `phone-sweden` | Sweden | `+46 70 XXX XX XX` | `+46 8 XXX XX XX` |
| `phone-norway` | Norway | `+47 4XX XX XXX` | `+47 XX XX XX XX` |
| `phone-denmark` | Denmark | `+45 XX XX XX XX` | `+45 XX XX XX XX` |
| `phone-finland` | Finland | `+358 4X XXX XXXX` | `+358 X XXX XXXX` |
| `phone-poland` | Poland | `+48 5XX XXX XXX` | `+48 XX XXX XX XX` |
| `phone-czech` | Czech Republic | `+420 6XX XXX XXX` | `+420 XXX XXX XXX` |
| `phone-hungary` | Hungary | `+36 30 XXX XXXX` | `+36 1 XXX XXXX` |
| `phone-ireland` | Ireland | `+353 8X XXX XXXX` | `+353 X XXX XXXX` |
| `phone-romania` | Romania | `+40 7XX XXX XXX` | `+40 XXX XXX XXX` |
| `phone-bulgaria` | Bulgaria | `+359 8X XXX XXXX` | `+359 X XXX XXXX` |
| `phone-croatia` | Croatia | `+385 9X XXX XXXX` | `+385 X XXX XXXX` |
| `phone-slovenia` | Slovenia | `+386 XX XXX XXX` | `+386 X XXX XX XX` |
| `phone-slovakia` | Slovakia | `+421 9XX XXX XXX` | `+421 XX XXX XXXX` |
| `phone-greece` | Greece | `+30 69X XXX XXXX` | `+30 21X XXX XXXX` |
| `phone-lithuania` | Lithuania | `+370 6XX XX XXX` | `+370 X XXX XXXX` |
| `phone-latvia` | Latvia | `+371 2X XXX XXX` | `+371 6XXX XXXX` |
| `phone-estonia` | Estonia | `+372 5XXX XXXX` | `+372 XXX XXXX` |
| `phone-luxembourg` | Luxembourg | `+352 6XX XXX XXX` | `+352 XX XX XX` |
| `phone-malta` | Malta | `+356 7XXX XXXX` | `+356 21XX XXXX` |
| `phone-cyprus` | Cyprus | `+357 9X XXX XXX` | `+357 2X XXX XXX` |
| `phone-iceland` | Iceland | `+354 XXX XXXX` | `+354 XXX XXXX` |
| `phone-european` | Generic European | Covers most EU formats with country codes |

##### European National Identity Cards (15+ Countries)
| Pattern Name | Country | Format | Example |
|--------------|---------|--------|---------|
| `spanish-dni` | Spain | 8 digits + letter | `12345678Z` |
| `spanish-nie` | Spain | Letter + 7 digits + letter | `X1234567L` |
| `french-cni` | France | 2 digits + 2 letters + 5 digits | `12AB34567` |
| `german-id` | Germany | 10 digits or letter + 8 digits | `1234567890` |
| `italian-cf` | Italy | 16 characters | `RSSMRA85M01H501Z` |
| `portuguese-cc` | Portugal | 8 digits + space + digit + space + 2 letters + digit | `12345678 9 AB0` |
| `dutch-bsn` | Netherlands | 9 digits | `123456789` |
| `belgian-nrn` | Belgium | YY.MM.DD-XXX.XX format | `85.07.30-097.23` |
| `austrian-svn` | Austria | DDMMYY/XXXX format | `300785/1234` |
| `swiss-ahv` | Switzerland | 756.XXXX.XXXX.XX format | `756.1234.5678.97` |
| `swedish-pn` | Sweden | YYMMDD-XXXX or YYYYMMDD-XXXX | `850730-1234` |
| `norwegian-fn` | Norway | DDMMYY-XXXXX | `300785-12345` |
| `danish-cpr` | Denmark | DDMMYY-XXXX | `300785-1234` |
| `finnish-ht` | Finland | DDMMYY±XXXX (± = century) | `300785A123X` |
| `uk-nino` | United Kingdom | 2 letters + 6 digits + letter | `AB123456C` |
| `irish-pps` | Ireland | 7 digits + 1-2 letters | `1234567W` |

#### Configuration

```yaml
pii-masking:
  enabled: true  # Enable/disable PII masking globally (default: true)
  mask-character: "*"  # Character to use for masking (default: *)
  preserve-length: true  # Preserve original length when masking (default: true)
  show-characters: 2  # Characters to show at start/end when preserve-length=false
  case-sensitive: false  # Case sensitivity for pattern matching (default: false)
  
  # Automatic logging protection
  auto-mask-logs: true  # Automatically mask PII in ALL application logs (default: true)
  
  # Control what gets masked
  mask-headers: true  # Mask headers (default: true)
  mask-bodies: true  # Mask request/response bodies (default: true) 
  mask-query-params: true  # Mask query parameters (default: true)
  mask-exceptions: true  # Mask exception messages (default: true)
  
  # Built-in patterns (can be overridden) - showing key examples
  patterns:
    email: "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b"
    phone-us: "\\b(?:\\+?1[-\\s]?)?(?:\\(?[0-9]{3}\\)?[-\\s]?)?[0-9]{3}[-\\s]?[0-9]{4}\\b"
    phone-spain: "\\b(?:\\+34[-\\s]?)?(?:[679][0-9]{2}[-\\s]?[0-9]{3}[-\\s]?[0-9]{3})\\b"
    phone-germany: "\\b(?:\\+49[-\\s]?)?(?:[1-9][0-9]{1,4}[-\\s]?[0-9]{3,}[-\\s]?[0-9]{3,})\\b"
    ssn: "\\b(?!000|666)[0-8][0-9]{2}-?(?!00)[0-9]{2}-?(?!0000)[0-9]{4}\\b"
    credit-card: "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3[0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b"
    spanish-dni: "\\b[0-9]{8}[A-Za-z]\\b"
    portuguese-cc: "\\b[0-9]{8}\\s[0-9]\\s[A-Za-z]{2}[0-9]\\b"
    dutch-bsn: "\\b[0-9]{9}\\b"
    jwt: "\\beyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*(?:\\.[a-zA-Z0-9_-]*)?\\b"
    api-key: "(?i)(?:api[_-]?key|token|secret)[\"'\\s]*[:=][\"'\\s]*(?!eyJ)[a-zA-Z0-9]{20,}"
    ip-address: "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
    mac-address: "\\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\\b"
    # Note: 39+ patterns available - see full list above
    
  # Custom patterns for your organization-specific data
  custom-patterns:
    internal-id: "ID-[0-9]{6}"  # Example: mask internal IDs
    account-number: "ACC-[0-9]{10}"  # Example: mask account numbers
    # Add more organization-specific patterns as needed
```

#### Usage Examples

The PII masking works automatically for ALL application logs once the library is included. Here are some examples:

##### Automatic Logging Protection

**Zero Configuration Required**: Simply include the library and all your application logs will automatically have PII data masked.

```java
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    public void processUser(User user) {
        // This log will automatically have PII data masked
        logger.info("Processing user: {} with email: {}", user.getName(), user.getEmail());
        
        // This will also be automatically masked
        logger.debug("User details: {}", user.toString());
        
        // Even exception messages are masked
        try {
            validateUser(user);
        } catch (Exception e) {
            logger.error("Validation failed for user {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
```

**Before automatic masking:**
```
2024-08-25 10:30:15.123 INFO [main] UserService - Processing user: John Doe with email: john.doe@example.com
2024-08-25 10:30:15.124 DEBUG [main] UserService - User details: User{name='John Doe', email='john.doe@example.com', phone='555-123-4567'}
2024-08-25 10:30:15.125 ERROR [main] UserService - Validation failed for user john.doe@example.com: Invalid phone format: 555-123-4567
```

**After automatic masking:**
```
2024-08-25 10:30:15.123 INFO [main] UserService - Processing user: John Doe with email: *********************
2024-08-25 10:30:15.124 DEBUG [main] UserService - User details: User{name='John Doe', email='*********************', phone='*************'}
2024-08-25 10:30:15.125 ERROR [main] UserService - Validation failed for user *********************: Invalid phone format: *************
```

**Partial reveal masking (preserve-length=false, show-characters=2):**
```
2024-08-25 10:30:15.123 INFO [main] UserService - Processing user: John Doe with email: jo*****************om
2024-08-25 10:30:15.124 DEBUG [main] UserService - User details: User{name='John Doe', email='jo*****************om', phone='55***********67'}
2024-08-25 10:30:15.125 ERROR [main] UserService - Validation failed for user jo*****************om: Invalid phone format: 55***********67
```

##### Manual Usage (Optional)

You can also use the PII masking service directly in your code when needed:

```java
@Service
public class MyService {
    
    private final PiiMaskingService piiMaskingService;
    
    public MyService(PiiMaskingService piiMaskingService) {
        this.piiMaskingService = piiMaskingService;
    }
    
    public void logSensitiveData(String data) {
        String maskedData = piiMaskingService.maskPiiData(data);
        log.info("Processing data: {}", maskedData);
    }
    
    public Map<String, String> maskHeaders(Map<String, String> headers) {
        return piiMaskingService.maskHeaders(headers);
    }
}
```


#### Performance Considerations

- Regex patterns are compiled once at startup and cached for optimal performance
- Thread-safe implementation suitable for high-concurrency applications
- Minimal memory overhead with efficient string processing
- Graceful degradation: if a pattern fails, masking continues with other patterns

#### Monitoring and Health

Check the PII masking service health:

```java
@RestController
public class HealthController {
    
    private final PiiMaskingService piiMaskingService;
    
    @GetMapping("/health/pii-masking")
    public Map<String, Object> getPiiMaskingHealth() {
        return piiMaskingService.getMaskingStats();
    }
}
```

Returns statistics like:
```json
{
  "enabled": true,
  "patternsLoaded": 39,
  "maskCharacter": "*",
  "preserveLength": true,
  "caseSensitive": false,
  "maskHeaders": true,
  "maskBodies": true,
  "maskQueryParams": true,
  "maskExceptions": true
}
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
    hazelcast:
      enabled: false  # Set to true to use Hazelcast for distributed caching
      map-name: idempotencyCache  # Hazelcast IMap name
    ehcache:
      enabled: false  # Set to true to use EhCache for local caching with persistence
      cache-name: idempotencyCache  # EhCache cache name
      disk-persistent: true  # Enable disk persistence

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
    com.firefly.common.web: INFO  # Set to DEBUG for detailed request logging
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
    @ConditionalOnProperty(value = "getfirefly.iomon.web.idempotency.cache-type", havingValue = "redis")
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