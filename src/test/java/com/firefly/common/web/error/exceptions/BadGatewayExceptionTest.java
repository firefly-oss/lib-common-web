package com.firefly.common.web.error.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BadGatewayExceptionTest {

    @Test
    void constructor_WithMessageAndServerInfo_SetsProperties() {
        // Arrange
        String message = "Bad gateway";
        String serverName = "PaymentService";
        String serverUrl = "https://api.payment.com";
        String serverErrorCode = "INVALID_RESPONSE";
        
        // Act
        BadGatewayException exception = new BadGatewayException(message, serverName, serverUrl, serverErrorCode);
        
        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("BAD_GATEWAY", exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(serverName, exception.getServerName());
        assertEquals(serverUrl, exception.getServerUrl());
        assertEquals(serverErrorCode, exception.getServerErrorCode());
    }
    
    @Test
    void constructor_WithCodeMessageAndServerInfo_SetsProperties() {
        // Arrange
        String code = "CUSTOM_BAD_GATEWAY";
        String message = "Bad gateway";
        String serverName = "PaymentService";
        String serverUrl = "https://api.payment.com";
        String serverErrorCode = "INVALID_RESPONSE";
        
        // Act
        BadGatewayException exception = new BadGatewayException(code, message, serverName, serverUrl, serverErrorCode);
        
        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(serverName, exception.getServerName());
        assertEquals(serverUrl, exception.getServerUrl());
        assertEquals(serverErrorCode, exception.getServerErrorCode());
    }
    
    @Test
    void forServer_WithServerInfo_ReturnsException() {
        // Arrange
        String serverName = "PaymentService";
        String serverUrl = "https://api.payment.com";
        String serverErrorCode = "INVALID_RESPONSE";
        
        // Act
        BadGatewayException exception = BadGatewayException.forServer(serverName, serverUrl, serverErrorCode);
        
        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("BAD_GATEWAY", exception.getCode());
        assertTrue(exception.getMessage().contains("Request to upstream server 'PaymentService' failed"));
        assertTrue(exception.getMessage().contains("INVALID_RESPONSE"));
        assertEquals(serverName, exception.getServerName());
        assertEquals(serverUrl, exception.getServerUrl());
        assertEquals(serverErrorCode, exception.getServerErrorCode());
        
        // Check metadata
        Map<String, Object> metadata = exception.getMetadata();
        assertNotNull(metadata);
        assertEquals(serverName, metadata.get("serverName"));
        assertEquals(serverUrl, metadata.get("serverUrl"));
        assertEquals(serverErrorCode, metadata.get("serverErrorCode"));
    }
    
    @Test
    void forServerWithMessage_WithServerInfoAndErrorMessage_ReturnsException() {
        // Arrange
        String serverName = "PaymentService";
        String serverUrl = "https://api.payment.com";
        String serverErrorCode = "INVALID_RESPONSE";
        String errorMessage = "Invalid payment details";
        
        // Act
        BadGatewayException exception = BadGatewayException.forServerWithMessage(serverName, serverUrl, serverErrorCode, errorMessage);
        
        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("BAD_GATEWAY", exception.getCode());
        assertTrue(exception.getMessage().contains("Request to upstream server 'PaymentService' failed"));
        assertTrue(exception.getMessage().contains("INVALID_RESPONSE"));
        assertTrue(exception.getMessage().contains("Invalid payment details"));
        assertEquals(serverName, exception.getServerName());
        assertEquals(serverUrl, exception.getServerUrl());
        assertEquals(serverErrorCode, exception.getServerErrorCode());
        
        // Check metadata
        Map<String, Object> metadata = exception.getMetadata();
        assertNotNull(metadata);
        assertEquals(serverName, metadata.get("serverName"));
        assertEquals(serverUrl, metadata.get("serverUrl"));
        assertEquals(serverErrorCode, metadata.get("serverErrorCode"));
        assertEquals(errorMessage, metadata.get("errorMessage"));
    }
    
    @Test
    void forInvalidResponse_WithServerInfoAndResponseStatus_ReturnsException() {
        // Arrange
        String serverName = "PaymentService";
        String serverUrl = "https://api.payment.com";
        int responseStatus = 500;
        
        // Act
        BadGatewayException exception = BadGatewayException.forInvalidResponse(serverName, serverUrl, responseStatus);
        
        // Assert
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("INVALID_UPSTREAM_RESPONSE", exception.getCode());
        assertTrue(exception.getMessage().contains("Upstream server 'PaymentService' returned an invalid response"));
        assertTrue(exception.getMessage().contains("500"));
        assertEquals(serverName, exception.getServerName());
        assertEquals(serverUrl, exception.getServerUrl());
        assertEquals("500", exception.getServerErrorCode());
        
        // Check metadata
        Map<String, Object> metadata = exception.getMetadata();
        assertNotNull(metadata);
        assertEquals(serverName, metadata.get("serverName"));
        assertEquals(serverUrl, metadata.get("serverUrl"));
        assertEquals(responseStatus, metadata.get("responseStatus"));
    }
}
