package com.catalis.common.web.error.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MethodNotAllowedExceptionTest {

    @Test
    void constructor_WithMessageAndMethods_SetsProperties() {
        // Arrange
        String message = "Method not allowed";
        HttpMethod method = HttpMethod.POST;
        Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.PUT);
        
        // Act
        MethodNotAllowedException exception = new MethodNotAllowedException(message, method, allowedMethods);
        
        // Assert
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatus());
        assertEquals("METHOD_NOT_ALLOWED", exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(method, exception.getMethod());
        assertEquals(allowedMethods, exception.getAllowedMethods());
    }
    
    @Test
    void constructor_WithCodeMessageAndMethods_SetsProperties() {
        // Arrange
        String code = "CUSTOM_METHOD_NOT_ALLOWED";
        String message = "Method not allowed";
        HttpMethod method = HttpMethod.POST;
        Set<HttpMethod> allowedMethods = Set.of(HttpMethod.GET, HttpMethod.PUT);
        
        // Act
        MethodNotAllowedException exception = new MethodNotAllowedException(code, message, method, allowedMethods);
        
        // Assert
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatus());
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
        assertEquals(method, exception.getMethod());
        assertEquals(allowedMethods, exception.getAllowedMethods());
    }
    
    @Test
    void forResource_WithMethodAndAllowedMethods_ReturnsException() {
        // Arrange
        HttpMethod method = HttpMethod.POST;
        HttpMethod[] allowedMethods = {HttpMethod.GET, HttpMethod.PUT};
        
        // Act
        MethodNotAllowedException exception = MethodNotAllowedException.forResource(method, allowedMethods);
        
        // Assert
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatus());
        assertEquals("METHOD_NOT_ALLOWED", exception.getCode());
        assertTrue(exception.getMessage().contains("Method POST is not allowed"));
        assertTrue(exception.getMessage().contains("[GET, PUT]"));
        assertEquals(method, exception.getMethod());
        assertEquals(Set.of(allowedMethods), exception.getAllowedMethods());
    }
    
    @Test
    void forResource_WithResourcePathMethodAndAllowedMethods_ReturnsException() {
        // Arrange
        String resourcePath = "/api/users";
        HttpMethod method = HttpMethod.POST;
        HttpMethod[] allowedMethods = {HttpMethod.GET, HttpMethod.PUT};
        
        // Act
        MethodNotAllowedException exception = MethodNotAllowedException.forResource(resourcePath, method, allowedMethods);
        
        // Assert
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, exception.getStatus());
        assertEquals("METHOD_NOT_ALLOWED", exception.getCode());
        assertTrue(exception.getMessage().contains("Method POST is not allowed"));
        assertTrue(exception.getMessage().contains("resource '/api/users'"));
        assertTrue(exception.getMessage().contains("[GET, PUT]"));
        assertEquals(method, exception.getMethod());
        assertEquals(Set.of(allowedMethods), exception.getAllowedMethods());
    }
}
