package com.catalis.common.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAPIConfigurationTest {

    @Test
    void customOpenAPI_WithDefaultValues() {
        // Arrange
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.acceptsProfiles(any(Profiles.class))).thenReturn(true);
        
        OpenAPIConfiguration configuration = new OpenAPIConfiguration(mockEnvironment);
        ReflectionTestUtils.setField(configuration, "applicationName", "Test Service");
        ReflectionTestUtils.setField(configuration, "applicationVersion", "1.0.0");
        ReflectionTestUtils.setField(configuration, "applicationDescription", "Test Description");
        ReflectionTestUtils.setField(configuration, "teamName", "Test Team");
        ReflectionTestUtils.setField(configuration, "teamEmail", "test@example.com");
        ReflectionTestUtils.setField(configuration, "teamUrl", "https://example.com");
        ReflectionTestUtils.setField(configuration, "licenseName", "Test License");
        ReflectionTestUtils.setField(configuration, "licenseUrl", "https://example.com/license");
        ReflectionTestUtils.setField(configuration, "contextPath", "/api");
        ReflectionTestUtils.setField(configuration, "serverPort", "8080");
        ReflectionTestUtils.setField(configuration, "securityEnabled", false);
        ReflectionTestUtils.setField(configuration, "serversEnabled", true);

        // Act
        OpenAPI result = configuration.customOpenAPI();

        // Assert
        assertNotNull(result);
        
        // Verify info
        Info info = result.getInfo();
        assertNotNull(info);
        assertEquals("Test Service API", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertEquals("Test Description", info.getDescription());
        
        // Verify contact
        Contact contact = info.getContact();
        assertNotNull(contact);
        assertEquals("Test Team", contact.getName());
        assertEquals("test@example.com", contact.getEmail());
        assertEquals("https://example.com", contact.getUrl());
        
        // Verify license
        License license = info.getLicense();
        assertNotNull(license);
        assertEquals("Test License", license.getName());
        assertEquals("https://example.com/license", license.getUrl());
        
        // Verify servers
        assertNotNull(result.getServers());
        assertFalse(result.getServers().isEmpty());
        
        // Verify no security
        assertNull(result.getComponents());
    }

    @Test
    void customOpenAPI_WithSecurityEnabled() {
        // Arrange
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        
        OpenAPIConfiguration configuration = new OpenAPIConfiguration(mockEnvironment);
        ReflectionTestUtils.setField(configuration, "applicationName", "Test Service");
        ReflectionTestUtils.setField(configuration, "applicationVersion", "1.0.0");
        ReflectionTestUtils.setField(configuration, "applicationDescription", "Test Description");
        ReflectionTestUtils.setField(configuration, "teamName", "Test Team");
        ReflectionTestUtils.setField(configuration, "teamEmail", "test@example.com");
        ReflectionTestUtils.setField(configuration, "teamUrl", "https://example.com");
        ReflectionTestUtils.setField(configuration, "licenseName", "Test License");
        ReflectionTestUtils.setField(configuration, "licenseUrl", "https://example.com/license");
        ReflectionTestUtils.setField(configuration, "contextPath", "/api");
        ReflectionTestUtils.setField(configuration, "serverPort", "8080");
        ReflectionTestUtils.setField(configuration, "securityEnabled", true);
        ReflectionTestUtils.setField(configuration, "serversEnabled", false);

        // Act
        OpenAPI result = configuration.customOpenAPI();

        // Assert
        assertNotNull(result);
        
        // Verify security
        assertNotNull(result.getComponents());
        assertNotNull(result.getComponents().getSecuritySchemes());
        assertTrue(result.getComponents().getSecuritySchemes().containsKey("bearer-jwt"));
        
        SecurityScheme securityScheme = result.getComponents().getSecuritySchemes().get("bearer-jwt");
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
        
        // Verify security requirement
        assertNotNull(result.getSecurity());
        assertFalse(result.getSecurity().isEmpty());
        SecurityRequirement securityRequirement = result.getSecurity().get(0);
        assertTrue(securityRequirement.containsKey("bearer-jwt"));
        
        // Verify no servers
        assertNull(result.getServers());
    }
}
