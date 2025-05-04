# Error Codes in lib-common-web

This document contains a complete list of all error codes used in the lib-common-web library, organized by exception type and HTTP code.

## Table of Contents

- [400 Bad Request](#400-bad-request)
- [401 Unauthorized](#401-unauthorized)
- [403 Forbidden](#403-forbidden)
- [404 Not Found](#404-not-found)
- [405 Method Not Allowed](#405-method-not-allowed)
- [408 Request Timeout](#408-request-timeout)
- [409 Conflict](#409-conflict)
- [410 Gone](#410-gone)
- [412 Precondition Failed](#412-precondition-failed)
- [413 Payload Too Large](#413-payload-too-large)
- [415 Unsupported Media Type](#415-unsupported-media-type)
- [423 Locked](#423-locked)
- [429 Too Many Requests](#429-too-many-requests)
- [500 Internal Server Error](#500-internal-server-error)
- [501 Not Implemented](#501-not-implemented)
- [502 Bad Gateway](#502-bad-gateway)
- [503 Service Unavailable](#503-service-unavailable)
- [504 Gateway Timeout](#504-gateway-timeout)

## 400 Bad Request

### InvalidRequestException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `INVALID_FIELD` | Invalid value for a specific field | `InvalidRequestException.forField(field, value, reason)` |
| `INVALID_REQUEST` | Invalid request (generic code) | `InvalidRequestException.withReason(code, message)` |

### DataIntegrityException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `UNIQUE_CONSTRAINT_VIOLATION` | Unique constraint violation | `DataIntegrityException.uniqueConstraintViolation(field, value)` |
| `FOREIGN_KEY_VIOLATION` | Foreign key violation | `DataIntegrityException.foreignKeyViolation(field, value)` |
| `DATA_INTEGRITY_VIOLATION` | Data integrity violation (generic) | `DataIntegrityException.withReason(code, message)` |

### ValidationException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `VALIDATION_ERROR` | Validation error with multiple fields | `new ValidationException.Builder().addError(...).build()` |

## 401 Unauthorized

### UnauthorizedException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `MISSING_AUTHENTICATION` | Authentication required | `UnauthorizedException.missingAuthentication()` |
| `INVALID_CREDENTIALS` | Invalid credentials | `UnauthorizedException.invalidCredentials()` |
| `TOKEN_EXPIRED` | Authentication token expired | `UnauthorizedException.tokenExpired()` |
| `INVALID_TOKEN` | Invalid authentication token | `UnauthorizedException.invalidToken(reason)` |

## 403 Forbidden

### ForbiddenException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `INSUFFICIENT_PERMISSIONS` | Insufficient permissions | `ForbiddenException.insufficientPermissions(requiredRole)` |
| `RESOURCE_ACCESS_DENIED` | Access denied to a resource | `ForbiddenException.resourceAccessDenied(resourceType, resourceId)` |

### AuthorizationException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `MISSING_PERMISSION` | Required permission not present | `AuthorizationException.missingPermission(permission)` |
| `AUTHORIZATION_ERROR` | Authorization error (generic) | `AuthorizationException.withReason(code, message)` |

## 404 Not Found

### ResourceNotFoundException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `RESOURCE_NOT_FOUND` | Resource not found | `ResourceNotFoundException.forResource(resourceType, resourceId)` |

## 405 Method Not Allowed

### MethodNotAllowedException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `METHOD_NOT_ALLOWED` | HTTP method not allowed | `MethodNotAllowedException.forResource(requestMethod, allowedMethods...)` |

## 408 Request Timeout

### OperationTimeoutException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `DATABASE_TIMEOUT` | Database operation timeout exceeded | `OperationTimeoutException.databaseTimeout(operation, timeoutMs)` |
| `SERVICE_CALL_TIMEOUT` | Service call timeout exceeded | `OperationTimeoutException.serviceCallTimeout(service, operation, timeoutMs)` |

## 409 Conflict

### ConflictException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `RESOURCE_ALREADY_EXISTS` | The resource already exists | `ConflictException.resourceAlreadyExists(resourceType, identifier)` |
| `CONFLICT` | Generic conflict | `ConflictException.withReason(code, message)` |

### ConcurrencyException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `OPTIMISTIC_LOCKING_FAILURE` | Optimistic locking failure | `ConcurrencyException.optimisticLockingFailure(resourceType, resourceId)` |
| `CONCURRENT_MODIFICATION` | Concurrent modification | `ConcurrencyException.withReason(code, message)` |

## 410 Gone

### GoneException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `RESOURCE_GONE` | Resource no longer available | `GoneException.forResource(resourceType, resourceId)` |
| `RESOURCE_DEPRECATED` | Deprecated resource | `GoneException.forResourceWithDetails(resourceType, resourceId, deprecationDate, details)` |

## 412 Precondition Failed

### PreconditionFailedException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `PRECONDITION_FAILED` | Precondition failed | `PreconditionFailedException.forIfMatch(expectedETag, actualETag)` |
| `IF_MATCH_FAILED` | If-Match condition failed | `PreconditionFailedException.forIfMatch(expectedETag, actualETag)` |
| `IF_NONE_MATCH_FAILED` | If-None-Match condition failed | `PreconditionFailedException.forIfNoneMatch(unexpectedETag)` |

## 413 Payload Too Large

### PayloadTooLargeException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `PAYLOAD_TOO_LARGE` | Payload too large | `PayloadTooLargeException.forSize(actualSize, maxSize)` |
| `MULTIPART_REQUEST_TOO_LARGE` | Multipart request too large | `PayloadTooLargeException.forMultipartRequest(maxSize, actualSize)` |
| `FILE_TOO_LARGE` | File too large | `PayloadTooLargeException.forFile(fileName, maxSize, actualSize)` |

## 415 Unsupported Media Type

### UnsupportedMediaTypeException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `UNSUPPORTED_MEDIA_TYPE` | Unsupported media type | `UnsupportedMediaTypeException.forMediaType(unsupportedType, supportedTypes...)` |

## 423 Locked

### LockedResourceException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `RESOURCE_LOCKED` | Resource locked | `LockedResourceException.forResource(resourceType, resourceId)` |
| `RESOURCE_LOCKED_BY_USER` | Resource locked by another user | `LockedResourceException.forResource(resourceType, resourceId, lockedByUser)` |

## 429 Too Many Requests

### RateLimitException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `RATE_LIMIT_EXCEEDED` | Rate limit exceeded | `RateLimitException.forUser(userId, retryAfterSeconds)` |
| `API_RATE_LIMIT_EXCEEDED` | API rate limit exceeded | `RateLimitException.forApi(apiName, retryAfterSeconds)` |

## 500 Internal Server Error

### ServiceException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `INTERNAL_ERROR` | Internal service error | `ServiceException.withCause(message, cause)` |
| `DEPENDENCY_FAILURE` | Dependency failure | `ServiceException.dependencyFailure(dependencyName, reason)` |

## 501 Not Implemented

### NotImplementedException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `NOT_IMPLEMENTED` | Feature not implemented | `NotImplementedException.forFeature(featureName)` |

## 502 Bad Gateway

### ThirdPartyServiceException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `SERVICE_ERROR` | External service error | `ThirdPartyServiceException.serviceError(serviceName, errorCode, errorMessage)` |
| `SERVICE_UNAVAILABLE` | External service unavailable | `ThirdPartyServiceException.serviceUnavailable(serviceName)` |

### BadGatewayException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `BAD_GATEWAY` | Communication error with external service | `BadGatewayException.forServer(serviceName, url, errorCode)` |

## 503 Service Unavailable

### ServiceUnavailableException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable | `ServiceUnavailableException.forService(serviceName)` |
| `SERVICE_UNAVAILABLE_WITH_RETRY` | Service unavailable with retry time | `ServiceUnavailableException.forServiceWithRetry(serviceName, retryAfterSeconds)` |

## 504 Gateway Timeout

### GatewayTimeoutException

| Code | Description | Factory Method |
|--------|-------------|-------------------|
| `GATEWAY_TIMEOUT` | External service timeout exceeded | `GatewayTimeoutException.forServer(serviceName, url, timeoutMs)` |
