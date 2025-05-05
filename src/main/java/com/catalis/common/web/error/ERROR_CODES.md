# Error Codes in lib-common-web

This document contains a complete list of all error codes used in the lib-common-web library, organized by exception type and HTTP code.

## Introduction

Error codes provide a standardized way to identify and handle errors across the application. Each error code is associated with a specific exception type and HTTP status code, making it easier to understand the nature of the error and how to handle it.

### Automatic Suggestions and Metadata

The library automatically adds helpful suggestions and relevant metadata to all exceptions. This is done through two aspects:

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

These aspects ensure that all error responses include helpful information for both users and developers.

### Error Response Format

All exceptions are converted to a standardized JSON response format:

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_FIELD",
  "message": "Invalid value for field 'email': must be a valid email format",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please provide a valid email address",
  "documentation": "https://api.example.com/docs/errors/invalid_field",
  "metadata": {
    "field": "email",
    "value": "invalid-email"
  }
}
```

### How to Use This Document

This document is organized by HTTP status codes. For each status code, you'll find:

1. A brief explanation of when this status code is used
2. The exception types associated with this status code
3. The error codes for each exception type
4. Common causes of the error
5. Recovery strategies for handling the error
6. Factory methods for creating the exception

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
- [Database Exceptions](#database-exceptions)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Best Practices](#best-practices)

## 400 Bad Request

The 400 Bad Request status code indicates that the server cannot process the request due to a client error (e.g., malformed request syntax, invalid request message framing, or deceptive request routing).

### InvalidRequestException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `INVALID_FIELD` | Invalid value for a specific field | • Field value doesn't match expected format<br>• Field value is out of allowed range<br>• Field contains invalid characters | • Check the field value against the required format<br>• Refer to API documentation for field requirements<br>• Validate input before sending | `InvalidRequestException.forField(field, value, reason)` |
| `INVALID_REQUEST` | Invalid request (generic code) | • Malformed JSON/XML<br>• Missing required fields<br>• Logical errors in request structure | • Verify request structure against API documentation<br>• Use a request validator before sending<br>• Check for typos in field names | `InvalidRequestException.withReason(code, message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_FIELD",
  "message": "Invalid value for field 'email': must be a valid email format",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "metadata": {
    "field": "email",
    "value": "invalid-email"
  }
}
```

### DataIntegrityException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `UNIQUE_CONSTRAINT_VIOLATION` | Unique constraint violation | • Attempting to create a resource with a duplicate unique field<br>• Updating a resource to have the same unique value as another resource | • Check if the resource already exists before creating<br>• Use a different value for the unique field<br>• Consider updating the existing resource instead | `DataIntegrityException.uniqueConstraintViolation(field, value)` |
| `FOREIGN_KEY_VIOLATION` | Foreign key violation | • Referencing a non-existent resource<br>• Deleting a resource that is referenced by other resources | • Verify that the referenced resource exists<br>• Create the referenced resource first<br>• Remove dependent resources before deleting the referenced resource | `DataIntegrityException.foreignKeyViolation(field, value)` |
| `DATA_INTEGRITY_VIOLATION` | Data integrity violation (generic) | • Database constraint violations<br>• Data type mismatches<br>• Invalid data relationships | • Check database schema constraints<br>• Ensure data types match expected formats<br>• Verify data relationships are valid | `DataIntegrityException.withReason(code, message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 400,
  "error": "Bad Request",
  "code": "UNIQUE_CONSTRAINT_VIOLATION",
  "message": "A user with email 'john.doe@example.com' already exists",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please use a different email address or log in with your existing account"
}
```

### ValidationException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `VALIDATION_ERROR` | Validation error with multiple fields | • Multiple fields with invalid values<br>• Form submission with several validation errors<br>• Batch validation failures | • Fix all reported validation errors<br>• Implement client-side validation<br>• Check error details for specific field issues | `new ValidationException.Builder().addError(...).build()` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for multiple fields",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "errors": [
    {
      "field": "email",
      "message": "must be a valid email format"
    },
    {
      "field": "password",
      "message": "must be at least 8 characters long"
    }
  ],
  "suggestion": "Please correct the validation errors and try again"
}
```

## 401 Unauthorized

The 401 Unauthorized status code indicates that the request lacks valid authentication credentials for the target resource. This status is sent with a WWW-Authenticate header that contains information on how to authorize correctly.

### UnauthorizedException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `MISSING_AUTHENTICATION` | Authentication required | • Request sent without authentication token<br>• Authentication header missing<br>• Session expired | • Include authentication token in request<br>• Ensure Authorization header is properly formatted<br>• Re-authenticate and obtain a new token | `UnauthorizedException.missingAuthentication()` |
| `INVALID_CREDENTIALS` | Invalid credentials | • Incorrect username/password<br>• Account does not exist<br>• Account locked or disabled | • Verify credentials are correct<br>• Check if account exists or is active<br>• Reset password if necessary | `UnauthorizedException.invalidCredentials()` |
| `TOKEN_EXPIRED` | Authentication token expired | • Token lifetime exceeded<br>• Session timeout<br>• Token revoked | • Refresh the token if a refresh token is available<br>• Re-authenticate to obtain a new token<br>• Implement automatic token refresh before expiration | `UnauthorizedException.tokenExpired()` |
| `INVALID_TOKEN` | Invalid authentication token | • Token tampered with<br>• Malformed token<br>• Token from different environment | • Ensure token is correctly formatted<br>• Re-authenticate to obtain a new token<br>• Verify you're using the token from the correct environment | `UnauthorizedException.invalidToken(reason)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 401,
  "error": "Unauthorized",
  "code": "TOKEN_EXPIRED",
  "message": "Authentication token has expired",
  "path": "/api/users/me",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please log in again to continue"
}
```

**Related Exceptions:**
- [ForbiddenException](#403-forbidden) - When the user is authenticated but lacks permission
- [AuthorizationException](#403-forbidden) - For more specific authorization errors

## 403 Forbidden

The 403 Forbidden status code indicates that the server understood the request but refuses to authorize it. Unlike 401, re-authenticating will make no difference. The access is permanently forbidden and tied to the application logic.

### ForbiddenException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `INSUFFICIENT_PERMISSIONS` | Insufficient permissions | • User lacks required role or permission<br>• Attempting to access admin-only functionality<br>• Role-based access control restriction | • Request access to the required role<br>• Use an account with appropriate permissions<br>• Contact administrator for role assignment | `ForbiddenException.insufficientPermissions(requiredRole)` |
| `RESOURCE_ACCESS_DENIED` | Access denied to a resource | • Attempting to access another user's resource<br>• Resource belongs to different organization<br>• Resource has specific access control | • Verify you're accessing the correct resource<br>• Request access to the resource<br>• Use appropriate resource ID for your context | `ForbiddenException.resourceAccessDenied(resourceType, resourceId)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 403,
  "error": "Forbidden",
  "code": "INSUFFICIENT_PERMISSIONS",
  "message": "You do not have the required role: ADMIN",
  "path": "/api/admin/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Contact your administrator to request access to this resource"
}
```

### AuthorizationException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `MISSING_PERMISSION` | Required permission not present | • User lacks specific permission<br>• Permission revoked<br>• Granular permission control failure | • Request the specific permission<br>• Check permission assignments in user profile<br>• Use an account with appropriate permissions | `AuthorizationException.missingPermission(permission)` |
| `AUTHORIZATION_ERROR` | Authorization error (generic) | • Complex authorization rule failure<br>• Time-based access restriction<br>• IP-based restriction | • Check specific error details in response<br>• Verify access conditions (time, location)<br>• Contact administrator for clarification | `AuthorizationException.withReason(code, message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 403,
  "error": "Forbidden",
  "code": "MISSING_PERMISSION",
  "message": "Missing required permission: USER_WRITE",
  "path": "/api/users/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Contact your administrator to request this permission"
}
```

**Related Exceptions:**
- [UnauthorizedException](#401-unauthorized) - When authentication is missing or invalid

## 404 Not Found

The 404 Not Found status code indicates that the server cannot find the requested resource. This could mean the URL is mistyped or the resource has been deleted or moved.

### ResourceNotFoundException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `RESOURCE_NOT_FOUND` | Resource not found | • Resource has been deleted<br>• Resource ID is incorrect<br>• Resource never existed<br>• Resource moved to a different location | • Verify the resource ID is correct<br>• Check if the resource has been moved or renamed<br>• Create the resource if it should exist<br>• Use search functionality to find similar resources | `ResourceNotFoundException.forResource(resourceType, resourceId)` |
| `ENTITY_NOT_FOUND` | Entity not found in database | • Database record does not exist<br>• Entity was deleted<br>• Query returned no results | • Verify the entity ID is correct<br>• Check if the entity has been deleted<br>• Create the entity if it should exist | `ResourceNotFoundException.withReason("ENTITY_NOT_FOUND", message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "message": "User with id '123' not found",
  "path": "/api/users/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Check that the ID is correct or the resource exists"
}
```

**Related Exceptions:**
- [GoneException](#410-gone) - When a resource existed but has been permanently removed

## 405 Method Not Allowed

The 405 Method Not Allowed status code indicates that the request method is known by the server but is not supported by the target resource. The server MUST generate an Allow header field in a 405 response containing a list of the target resource's currently supported methods.

### MethodNotAllowedException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `METHOD_NOT_ALLOWED` | HTTP method not allowed | • Using POST on a read-only endpoint<br>• Using DELETE on a resource that can't be deleted<br>• Using PUT instead of PATCH for partial updates | • Check the API documentation for allowed methods<br>• Use the correct HTTP method for the operation<br>• Check the Allow header in the response for supported methods | `MethodNotAllowedException.forResource(requestMethod, allowedMethods...)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 405,
  "error": "Method Not Allowed",
  "code": "METHOD_NOT_ALLOWED",
  "message": "Method DELETE not allowed for this resource. Allowed methods: GET, POST, PUT",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Use one of the allowed methods: GET, POST, PUT"
}
```

**HTTP Headers:**
```
Allow: GET, POST, PUT
```

## 408 Request Timeout

The 408 Request Timeout status code indicates that the server did not receive a complete request message within the time that it was prepared to wait. The client MAY repeat the request without modifications at any later time.

### OperationTimeoutException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `DATABASE_TIMEOUT` | Database operation timeout exceeded | • Complex query taking too long<br>• Database under heavy load<br>• Query missing proper indexes | • Retry the request after a short delay<br>• Optimize the database query<br>• Break down complex operations into smaller ones | `OperationTimeoutException.databaseTimeout(operation, timeoutMs)` |
| `SERVICE_CALL_TIMEOUT` | Service call timeout exceeded | • Dependent service is slow<br>• Network latency issues<br>• Service under heavy load | • Implement retry with exponential backoff<br>• Check the status of the dependent service<br>• Consider increasing the timeout for critical operations | `OperationTimeoutException.serviceCallTimeout(service, operation, timeoutMs)` |
| `OPERATION_TIMEOUT` | Generic operation timeout | • Long-running computation<br>• Resource contention<br>• System under heavy load | • Retry the operation<br>• Consider asynchronous processing for long operations<br>• Optimize the operation for better performance | `new OperationTimeoutException(code, message, operation, timeoutMs)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 408,
  "error": "Request Timeout",
  "code": "DATABASE_TIMEOUT",
  "message": "Database operation 'findUserTransactions' timed out after 5000ms",
  "path": "/api/users/123/transactions",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Try again with a more specific query or fewer results",
  "metadata": {
    "operation": "findUserTransactions",
    "timeoutMs": 5000
  }
}
```

**Related Exceptions:**
- [GatewayTimeoutException](#504-gateway-timeout) - When an external service times out

## 409 Conflict

The 409 Conflict status code indicates that the request could not be completed due to a conflict with the current state of the target resource. This code is used when the user might be able to resolve the conflict and resubmit the request.

### ConflictException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `RESOURCE_ALREADY_EXISTS` | The resource already exists | • Creating a resource with an ID that already exists<br>• Creating a resource with a unique field that already exists<br>• Duplicate resource creation | • Check if the resource exists before creating<br>• Use a different identifier<br>• Update the existing resource instead of creating a new one | `ConflictException.resourceAlreadyExists(resourceType, identifier)` |
| `CONFLICT` | Generic conflict | • State transition not allowed<br>• Business rule violation<br>• Logical conflict in the request | • Check the current state of the resource<br>• Verify business rules and constraints<br>• Modify the request to comply with business rules | `ConflictException.withReason(code, message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 409,
  "error": "Conflict",
  "code": "RESOURCE_ALREADY_EXISTS",
  "message": "User with email 'john.doe@example.com' already exists",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Use a different email address or update the existing user"
}
```

### ConcurrencyException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `OPTIMISTIC_LOCKING_FAILURE` | Optimistic locking failure | • Two users editing the same resource simultaneously<br>• Stale version number or ETag<br>• Resource modified since it was retrieved | • Refresh the resource to get the latest version<br>• Merge your changes with the latest version<br>• Implement conflict resolution UI for users | `ConcurrencyException.optimisticLockingFailure(resourceType, resourceId)` |
| `CONCURRENT_MODIFICATION` | Concurrent modification | • Race condition in resource modification<br>• Parallel requests modifying the same resource<br>• Batch operation conflicts | • Implement retry logic with exponential backoff<br>• Use pessimistic locking for critical operations<br>• Sequence operations that modify the same resource | `ConcurrencyException.withReason(code, message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 409,
  "error": "Conflict",
  "code": "OPTIMISTIC_LOCKING_FAILURE",
  "message": "The resource 'User' with id '123' has been modified since you retrieved it",
  "path": "/api/users/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Refresh the resource and try again with the updated version",
  "metadata": {
    "resourceType": "User",
    "resourceId": "123",
    "expectedVersion": 2,
    "actualVersion": 3
  }
}
```

**Related Exceptions:**
- [DataIntegrityException](#400-bad-request) - For database constraint violations
- [LockedResourceException](#423-locked) - When a resource is explicitly locked

## 410 Gone

The 410 Gone status code indicates that the target resource is no longer available at the server and no forwarding address is known. This condition is expected to be permanent. Clients with link-editing capabilities SHOULD delete references to the Request-URI after user approval.

### GoneException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `RESOURCE_GONE` | Resource no longer available | • Resource has been permanently deleted<br>• API endpoint has been removed<br>• Content has been taken down | • Remove references to this resource<br>• Use alternative resources if available<br>• Update client to use newer API versions | `GoneException.forResource(resourceType, resourceId)` |
| `RESOURCE_DEPRECATED` | Deprecated resource | • Using an old API version<br>• Feature has been sunset<br>• Resource has been replaced | • Migrate to the new API version<br>• Check documentation for replacement resources<br>• Update client implementation | `GoneException.forResourceWithDetails(resourceType, resourceId, deprecationDate, details)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 410,
  "error": "Gone",
  "code": "RESOURCE_DEPRECATED",
  "message": "The API endpoint '/api/v1/users' has been deprecated and is no longer available",
  "path": "/api/v1/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please use the new endpoint at /api/v2/users",
  "metadata": {
    "resourceType": "ApiEndpoint",
    "resourceId": "/api/v1/users",
    "deprecationDate": "2023-01-15",
    "replacementUrl": "/api/v2/users"
  }
}
```

**Related Exceptions:**
- [ResourceNotFoundException](#404-not-found) - When a resource doesn't exist (but wasn't explicitly removed)

## 412 Precondition Failed

The 412 Precondition Failed status code indicates that one or more conditions given in the request header fields evaluated to false when tested on the server. This is used to prevent the 'lost update' problem, where a client GETs a resource's state, modifies it, and PUTs it back, but the resource has been modified by another party in the meantime.

### PreconditionFailedException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `PRECONDITION_FAILED` | Precondition failed | • Resource state doesn't match expected state<br>• Conditional request failed<br>• Business rule precondition not met | • Refresh the resource to get current state<br>• Update your preconditions based on current state<br>• Verify business rules are satisfied | `PreconditionFailedException.forIfMatch(expectedETag, actualETag)` |
| `IF_MATCH_FAILED` | If-Match condition failed | • Resource has been modified since retrieval<br>• ETag doesn't match current resource state<br>• Attempting to update a stale resource | • Fetch the latest version of the resource<br>• Merge your changes with the latest version<br>• Update your If-Match header with the current ETag | `PreconditionFailedException.forIfMatch(expectedETag, actualETag)` |
| `IF_NONE_MATCH_FAILED` | If-None-Match condition failed | • Resource already exists when creating<br>• Attempting to create a duplicate resource<br>• Conditional GET with matching ETag | • Use PUT to update the existing resource instead<br>• Check if the resource already exists before creating<br>• For GET requests, use the cached version | `PreconditionFailedException.forIfNoneMatch(unexpectedETag)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 412,
  "error": "Precondition Failed",
  "code": "IF_MATCH_FAILED",
  "message": "The resource has been modified since you last retrieved it",
  "path": "/api/users/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Fetch the latest version and try again",
  "metadata": {
    "expectedETag": "\"abc123\"",
    "actualETag": "\"def456\""
  }
}
```

**HTTP Headers Example:**
```
If-Match: "abc123"
ETag: "def456"
```

**Related Exceptions:**
- [ConcurrencyException](#409-conflict) - For optimistic locking failures
- [ConflictException](#409-conflict) - For other state conflicts

## 413 Payload Too Large

The 413 Payload Too Large status code indicates that the server is refusing to process a request because the request payload is larger than the server is willing or able to process. The server MAY close the connection to prevent the client from continuing the request.

### PayloadTooLargeException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `PAYLOAD_TOO_LARGE` | Payload too large | • Request body exceeds size limit<br>• JSON/XML payload too large<br>• Batch operation with too many items | • Reduce the size of the request payload<br>• Split large requests into smaller chunks<br>• Use pagination for large data sets | `PayloadTooLargeException.forSize(actualSize, maxSize)` |
| `MULTIPART_REQUEST_TOO_LARGE` | Multipart request too large | • Total size of all parts exceeds limit<br>• Too many parts in multipart request<br>• Form submission with large attachments | • Reduce the size of the multipart request<br>• Upload files separately<br>• Compress files before uploading | `PayloadTooLargeException.forMultipartRequest(maxSize, actualSize)` |
| `FILE_TOO_LARGE` | File too large | • Uploaded file exceeds size limit<br>• Image or document too large<br>• Video or audio file too large | • Compress the file before uploading<br>• Resize images to smaller dimensions<br>• Split large files into smaller chunks<br>• Use a file streaming service for large files | `PayloadTooLargeException.forFile(fileName, maxSize, actualSize)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 413,
  "error": "Payload Too Large",
  "code": "FILE_TOO_LARGE",
  "message": "The uploaded file 'large-image.jpg' (15MB) exceeds the maximum allowed size (10MB)",
  "path": "/api/files/upload",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please reduce the file size and try again",
  "metadata": {
    "fileName": "large-image.jpg",
    "actualSize": 15728640,
    "maxSize": 10485760,
    "sizeUnit": "bytes"
  }
}
```

**Related Exceptions:**
- [InvalidRequestException](#400-bad-request) - For other request format issues

## 415 Unsupported Media Type

The 415 Unsupported Media Type status code indicates that the server is refusing to service the request because the payload is in a format not supported by this method on the target resource. The format problem might be due to the request's indicated Content-Type or Content-Encoding, or as a result of inspecting the data directly.

### UnsupportedMediaTypeException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `UNSUPPORTED_MEDIA_TYPE` | Unsupported media type | • Using XML when only JSON is supported<br>• Incorrect Content-Type header<br>• Unsupported file format<br>• Missing or invalid Content-Type header | • Check API documentation for supported formats<br>• Use the correct Content-Type header<br>• Convert your payload to a supported format<br>• For file uploads, use a supported file format | `UnsupportedMediaTypeException.forMediaType(unsupportedType, supportedTypes...)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 415,
  "error": "Unsupported Media Type",
  "code": "UNSUPPORTED_MEDIA_TYPE",
  "message": "Content type 'application/xml' is not supported. Supported types: application/json",
  "path": "/api/users",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Use 'application/json' Content-Type header and format your request body as JSON",
  "metadata": {
    "unsupportedType": "application/xml",
    "supportedTypes": ["application/json"]
  }
}
```

**HTTP Headers Example:**
```
Content-Type: application/xml
```

**Related Exceptions:**
- [InvalidRequestException](#400-bad-request) - For malformed request bodies

## 423 Locked

The 423 Locked status code indicates that the resource that is being accessed is locked. This is commonly used in WebDAV to indicate that a resource is locked by another user or process, preventing modifications until the lock is released.

### LockedResourceException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `RESOURCE_LOCKED` | Resource locked | • Resource is being edited by a process<br>• Resource has an explicit lock<br>• System maintenance lock | • Wait for the lock to be released<br>• Request notification when the resource is unlocked<br>• Try again later | `LockedResourceException.forResource(resourceType, resourceId)` |
| `RESOURCE_LOCKED_BY_USER` | Resource locked by another user | • Another user is editing the resource<br>• Collaborative editing conflict<br>• User has explicitly locked the resource | • Contact the user who has the lock<br>• Wait for the user to finish editing<br>• Request the lock to be released<br>• Use a collaborative editing solution | `LockedResourceException.forResource(resourceType, resourceId, lockedByUser)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 423,
  "error": "Locked",
  "code": "RESOURCE_LOCKED_BY_USER",
  "message": "The document 'quarterly-report.docx' is currently locked by user 'jane.doe@example.com'",
  "path": "/api/documents/quarterly-report.docx",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Contact the user or wait until they finish editing",
  "metadata": {
    "resourceType": "Document",
    "resourceId": "quarterly-report.docx",
    "lockedBy": "jane.doe@example.com",
    "lockedSince": "2023-05-12T13:45:30.000Z",
    "lockExpiresAt": "2023-05-12T14:45:30.000Z"
  }
}
```

**Related Exceptions:**
- [ConcurrencyException](#409-conflict) - For optimistic locking failures

## 429 Too Many Requests

The 429 Too Many Requests status code indicates that the user has sent too many requests in a given amount of time ("rate limiting"). The response should include details explaining the condition and might include a Retry-After header indicating how long to wait before making a new request.

### RateLimitException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `RATE_LIMIT_EXCEEDED` | Rate limit exceeded | • Too many requests from a single user<br>• Client sending requests too quickly<br>• Automated scripts without rate limiting | • Implement exponential backoff<br>• Respect the Retry-After header<br>• Reduce request frequency<br>• Batch requests when possible | `RateLimitException.forUser(userId, retryAfterSeconds)` |
| `API_RATE_LIMIT_EXCEEDED` | API rate limit exceeded | • Exceeding API quota<br>• Shared API key with too many users<br>• API plan limitations | • Upgrade to a higher API tier<br>• Distribute load across multiple API keys<br>• Implement caching to reduce API calls<br>• Optimize API usage patterns | `RateLimitException.forApi(apiName, retryAfterSeconds)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 429,
  "error": "Too Many Requests",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. You have made too many requests. Please try again in 60 seconds.",
  "path": "/api/search",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Implement rate limiting in your client application",
  "metadata": {
    "userId": "user123",
    "limit": 100,
    "remaining": 0,
    "resetAt": "2023-05-12T14:33:25.000Z",
    "retryAfterSeconds": 60
  }
}
```

**HTTP Headers Example:**
```
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1683901405
```

**Related Exceptions:**
- [ServiceUnavailableException](#503-service-unavailable) - When service is temporarily unavailable

## 500 Internal Server Error

The 500 Internal Server Error status code indicates that the server encountered an unexpected condition that prevented it from fulfilling the request. This is a generic error message when no more specific message is suitable.

### ServiceException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `INTERNAL_ERROR` | Internal service error | • Unhandled exception in server code<br>• Database connection failure<br>• Out of memory error<br>• Configuration error | • Report the error with the trace ID<br>• Try again later<br>• Check service status page<br>• Contact support if persistent | `ServiceException.withCause(message, cause)` |
| `DEPENDENCY_FAILURE` | Dependency failure | • Required service is down<br>• Database unavailable<br>• Third-party API failure<br>• Network partition | • Check the status of the dependency<br>• Try again later<br>• Use fallback mechanism if available<br>• Implement circuit breaker pattern | `ServiceException.dependencyFailure(dependencyName, reason)` |
| `DATABASE_ERROR` | Database error | • SQL syntax error<br>• Database connection issue<br>• Transaction rollback<br>• Deadlock detected | • Check database connectivity<br>• Verify SQL queries<br>• Implement retry with backoff<br>• Report the issue with details | `ServiceException.withReason("DATABASE_ERROR", message)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 500,
  "error": "Internal Server Error",
  "code": "DEPENDENCY_FAILURE",
  "message": "Failed to process request due to dependency failure: Payment Service is unavailable",
  "path": "/api/orders",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please try again later or contact support with the trace ID",
  "metadata": {
    "dependencyName": "PaymentService",
    "reason": "Connection refused"
  }
}
```

**Related Exceptions:**
- [ThirdPartyServiceException](#502-bad-gateway) - For external service errors
- [ServiceUnavailableException](#503-service-unavailable) - When service is temporarily unavailable

## 501 Not Implemented

The 501 Not Implemented status code indicates that the server does not support the functionality required to fulfill the request. This is the appropriate response when the server does not recognize the request method and is not capable of supporting it for any resource.

### NotImplementedException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `NOT_IMPLEMENTED` | Feature not implemented | • API endpoint is planned but not yet implemented<br>• Feature is documented but not available<br>• Using a method that's not supported by the server | • Check API documentation for available features<br>• Use an alternative endpoint or method<br>• Contact the API provider for feature timeline<br>• Check for beta/preview versions of the feature | `NotImplementedException.forFeature(featureName)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 501,
  "error": "Not Implemented",
  "code": "NOT_IMPLEMENTED",
  "message": "The requested feature 'batch processing' is not implemented yet",
  "path": "/api/batch",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please process items individually or check back later",
  "metadata": {
    "featureName": "batch processing",
    "plannedReleaseDate": "2023-Q3"
  }
}
```

## 502 Bad Gateway

The 502 Bad Gateway status code indicates that the server, while acting as a gateway or proxy, received an invalid response from an inbound server it accessed while attempting to fulfill the request.

### ThirdPartyServiceException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `SERVICE_ERROR` | External service error | • Third-party API returned an error<br>• External service is malfunctioning<br>• Invalid response from external service | • Check external service status<br>• Verify API request format<br>• Implement retry with exponential backoff<br>• Use circuit breaker pattern | `ThirdPartyServiceException.serviceError(serviceName, errorCode, errorMessage)` |
| `SERVICE_UNAVAILABLE` | External service unavailable | • External service is down<br>• Network connectivity issues<br>• Service maintenance window | • Check service status page<br>• Try again later<br>• Use fallback mechanism if available<br>• Implement graceful degradation | `ThirdPartyServiceException.serviceUnavailable(serviceName)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 502,
  "error": "Bad Gateway",
  "code": "SERVICE_ERROR",
  "message": "Payment service returned an error: Invalid card number",
  "path": "/api/payments",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Verify your payment details and try again",
  "metadata": {
    "serviceName": "PaymentService",
    "errorCode": "INVALID_CARD",
    "errorMessage": "The card number provided is invalid"
  }
}
```

### BadGatewayException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `BAD_GATEWAY` | Communication error with external service | • Invalid response format from upstream server<br>• Protocol error in communication<br>• Proxy/gateway configuration issue | • Check network connectivity<br>• Verify service compatibility<br>• Check proxy/gateway configuration<br>• Implement retry mechanism | `BadGatewayException.forServer(serviceName, url, errorCode)` |
| `UNKNOWN_HOST` | Unknown host error | • DNS resolution failure<br>• Incorrect service hostname<br>• Network configuration issue | • Verify the hostname is correct<br>• Check DNS configuration<br>• Verify network connectivity<br>• Check firewall settings | `BadGatewayException.forServer(serviceName, url, "UNKNOWN_HOST")` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 502,
  "error": "Bad Gateway",
  "code": "BAD_GATEWAY",
  "message": "Error communicating with external service: Payment Gateway",
  "path": "/api/checkout",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please try again later or contact support if the issue persists",
  "metadata": {
    "serviceName": "PaymentGateway",
    "url": "https://payments.example.com/api/v1/process",
    "errorCode": "CONNECTION_REFUSED"
  }
}
```

**Related Exceptions:**
- [ServiceUnavailableException](#503-service-unavailable) - When service is temporarily unavailable
- [GatewayTimeoutException](#504-gateway-timeout) - When external service times out

## 503 Service Unavailable

The 503 Service Unavailable status code indicates that the server is currently unable to handle the request due to a temporary overload or scheduled maintenance, which will likely be alleviated after some delay. The server MAY send a Retry-After header field to suggest an appropriate retry time.

### ServiceUnavailableException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `SERVICE_UNAVAILABLE` | Service temporarily unavailable | • Server is overloaded<br>• System under maintenance<br>• Service is starting up<br>• Resource exhaustion | • Try again later<br>• Implement exponential backoff<br>• Check service status page<br>• Reduce request load | `ServiceUnavailableException.forService(serviceName)` |
| `SERVICE_UNAVAILABLE_WITH_RETRY` | Service unavailable with retry time | • Scheduled maintenance window<br>• Temporary capacity issues<br>• Rolling deployment in progress | • Retry after the specified time<br>• Respect the Retry-After header<br>• Schedule requests for after maintenance<br>• Use alternative service if available | `ServiceUnavailableException.forServiceWithRetry(serviceName, retryAfterSeconds)` |
| `MAINTENANCE_MODE` | Service in maintenance mode | • Scheduled system maintenance<br>• Database migration<br>• Major version upgrade | • Check maintenance schedule<br>• Retry after maintenance window<br>• Plan for downtime during maintenance periods | `ServiceUnavailableException.maintenanceMode(serviceName, endTimeMillis)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 503,
  "error": "Service Unavailable",
  "code": "SERVICE_UNAVAILABLE_WITH_RETRY",
  "message": "The service 'OrderService' is temporarily unavailable due to maintenance",
  "path": "/api/orders",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please try again after 15 minutes",
  "metadata": {
    "serviceName": "OrderService",
    "retryAfterSeconds": 900,
    "maintenanceEndTime": "2023-05-12T15:00:00.000Z"
  }
}
```

**HTTP Headers Example:**
```
Retry-After: 900
```

**Related Exceptions:**
- [ThirdPartyServiceException](#502-bad-gateway) - When an external service is unavailable
- [RateLimitException](#429-too-many-requests) - When rate limits are exceeded

## 504 Gateway Timeout

The 504 Gateway Timeout status code indicates that the server, while acting as a gateway or proxy, did not receive a timely response from an upstream server it needed to access in order to complete the request.

### GatewayTimeoutException

| Code | Description | Common Causes | Recovery Strategy | Factory Method |
|--------|-------------|----------------|-------------------|-------------------|
| `GATEWAY_TIMEOUT` | External service timeout exceeded | • Upstream service is slow<br>• Network latency issues<br>• Complex operation taking too long<br>• External service overloaded | • Retry the request<br>• Implement exponential backoff<br>• Increase timeout for complex operations<br>• Check external service status | `GatewayTimeoutException.forServer(serviceName, url, timeoutMs)` |
| `READ_TIMEOUT` | Read timeout from external service | • External service started processing but didn't respond in time<br>• Large response payload<br>• Network congestion | • Retry with longer timeout<br>• Request smaller data chunks<br>• Implement pagination for large datasets<br>• Use asynchronous processing for long operations | `GatewayTimeoutException.forReadTimeout(serviceName, url, timeoutMs)` |
| `CONNECTION_TIMEOUT` | Connection timeout to external service | • External service unreachable<br>• Network routing issues<br>• Firewall blocking connection | • Check network connectivity<br>• Verify firewall settings<br>• Check if service is running<br>• Try alternative connection routes | `GatewayTimeoutException.forConnectionTimeout(serviceName, url, timeoutMs)` |

**Example Response:**

```json
{
  "timestamp": "12/05/2023T14:32:25.123456",
  "status": 504,
  "error": "Gateway Timeout",
  "code": "GATEWAY_TIMEOUT",
  "message": "Request to external service 'InventoryService' timed out after 30000ms",
  "path": "/api/products/availability",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "suggestion": "Please try again later when the service is more responsive",
  "metadata": {
    "serviceName": "InventoryService",
    "url": "https://inventory.example.com/api/v1/check",
    "timeoutMs": 30000
  }
}
```

**Related Exceptions:**
- [OperationTimeoutException](#408-request-timeout) - For internal operation timeouts
- [ServiceUnavailableException](#503-service-unavailable) - When service is temporarily unavailable

## Database Exceptions

This section covers exceptions related to database operations, including both JPA and R2DBC exceptions that are automatically converted to business exceptions.

### R2DBC Exceptions

| Original Exception | Converted To | Common Causes | Recovery Strategy |
|--------|-------------|----------------|-------------------|
| `R2dbcTimeoutException` | `OperationTimeoutException` | • Long-running query<br>• Database under heavy load<br>• Missing indexes | • Optimize the query<br>• Add appropriate indexes<br>• Implement retry with backoff |
| `R2dbcRollbackException` | `ConcurrencyException` | • Transaction rollback<br>• Deadlock detected<br>• Serialization failure | • Retry the transaction<br>• Reduce transaction scope<br>• Implement optimistic locking |
| `R2dbcDataIntegrityViolationException` | `DataIntegrityException` | • Unique constraint violation<br>• Foreign key violation<br>• Check constraint violation | • Validate data before saving<br>• Handle constraint violations gracefully<br>• Check for existing records before inserting |
| `R2dbcPermissionDeniedException` | `ForbiddenException` | • Insufficient database permissions<br>• Role-based access control<br>• Row-level security | • Check database user permissions<br>• Use appropriate database role<br>• Verify security policies |
| `R2dbcBadGrammarException` | `InvalidRequestException` | • SQL syntax error<br>• Invalid query structure<br>• Using wrong dialect | • Validate SQL queries<br>• Use parameterized queries<br>• Check for SQL injection vulnerabilities |
| `R2dbcTransientException` | `ServiceException` | • Temporary database error<br>• Connection issues<br>• Server overload | • Implement retry with exponential backoff<br>• Check database connectivity<br>• Monitor database performance |
| `R2dbcNonTransientException` | `ServiceException` | • Permanent database error<br>• Configuration issues<br>• Database corruption | • Check database configuration<br>• Verify database integrity<br>• Contact database administrator |

### JPA Exceptions

| Original Exception | Converted To | Common Causes | Recovery Strategy |
|--------|-------------|----------------|-------------------|
| `EntityNotFoundException` | `ResourceNotFoundException` | • Entity with ID doesn't exist<br>• Entity was deleted<br>• Reference to non-existent entity | • Check if entity exists before referencing<br>• Handle not found cases gracefully<br>• Use Optional return types |
| `EntityExistsException` | `DataIntegrityException` | • Attempting to persist an entity that already exists<br>• Duplicate primary key<br>• Unique constraint violation | • Check if entity exists before persisting<br>• Use merge instead of persist when appropriate<br>• Handle duplicate key cases |
| `QueryTimeoutException` | `OperationTimeoutException` | • Query taking too long<br>• Database under heavy load<br>• Complex join operations | • Optimize the query<br>• Add appropriate indexes<br>• Use pagination for large result sets |
| `PersistenceException` | `ServiceException` or specific subtype | • General database errors<br>• Connection issues<br>• Transaction problems | • Check database connectivity<br>• Implement retry mechanism<br>• Log detailed error information |

## Troubleshooting Guide

### Common Error Patterns and Solutions

#### Authentication and Authorization Issues

1. **Symptom**: Receiving `401 Unauthorized` errors
   - **Check**: Is your authentication token included and valid?
   - **Solution**: Refresh your token or re-authenticate

2. **Symptom**: Receiving `403 Forbidden` errors despite being authenticated
   - **Check**: Do you have the required permissions?
   - **Solution**: Request additional permissions or use an account with appropriate access

#### Data Validation Issues

1. **Symptom**: Receiving `400 Bad Request` with validation errors
   - **Check**: Are all required fields provided with valid values?
   - **Solution**: Validate input on the client side before submitting

2. **Symptom**: Receiving `409 Conflict` when creating resources
   - **Check**: Does the resource already exist?
   - **Solution**: Check for existence before creating or use PUT to update instead

#### Performance Issues

1. **Symptom**: Receiving `408 Request Timeout` or `504 Gateway Timeout`
   - **Check**: Are you requesting too much data or performing complex operations?
   - **Solution**: Use pagination, optimize queries, or break down complex operations

2. **Symptom**: Receiving `429 Too Many Requests`
   - **Check**: Are you sending requests too frequently?
   - **Solution**: Implement rate limiting and respect Retry-After headers

### Debugging Strategies

1. **Check the error code and message**: The error response contains valuable information about what went wrong
2. **Examine the trace ID**: Include the trace ID when reporting issues to support
3. **Look at the metadata**: Additional context is often provided in the metadata field
4. **Follow the suggestion**: Most error responses include a suggestion for how to resolve the issue
5. **Check related documentation**: The documentation link can provide more detailed information

## Best Practices

### Error Handling in Client Applications

1. **Implement robust error handling**: Don't assume requests will always succeed
2. **Parse error responses**: Extract the error code, message, and metadata
3. **Display user-friendly messages**: Translate error codes to user-friendly messages
4. **Log detailed error information**: Include trace IDs for troubleshooting
5. **Implement retry mechanisms**: Use exponential backoff for transient errors
6. **Respect rate limits**: Honor Retry-After headers and implement client-side throttling
7. **Validate input before sending**: Prevent validation errors before they happen
8. **Handle specific error types**: Implement specialized handling for different error codes
9. **Degrade gracefully**: Provide fallback behavior when services are unavailable
10. **Monitor error rates**: Track error patterns to identify systemic issues

### Preventing Common Errors

1. **Validate input thoroughly**: Check data types, formats, and constraints
2. **Use idempotent operations**: Make operations safe to retry
3. **Implement concurrency control**: Use ETags or version numbers for optimistic locking
4. **Check resource existence**: Verify resources exist before referencing them
5. **Handle database constraints**: Check for unique constraints before inserting
6. **Manage authentication properly**: Refresh tokens before they expire
7. **Implement proper authorization**: Check permissions before attempting operations
8. **Use pagination for large datasets**: Avoid timeout and payload size issues
9. **Implement circuit breakers**: Prevent cascading failures when dependencies fail
10. **Monitor and alert**: Detect and respond to error patterns quickly
