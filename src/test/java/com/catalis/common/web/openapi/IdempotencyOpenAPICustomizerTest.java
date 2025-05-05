package com.catalis.common.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the IdempotencyOpenAPICustomizer class.
 */
class IdempotencyOpenAPICustomizerTest {

    private IdempotencyOpenAPICustomizer customizer;
    private OpenAPI openAPI;
    private Paths paths;
    private PathItem pathItem;
    private Operation postOperation;
    private Operation putOperation;
    private Operation patchOperation;
    private Operation getOperation;

    @BeforeEach
    void setUp() {
        customizer = new IdempotencyOpenAPICustomizer();
        
        // Set up OpenAPI structure
        openAPI = new OpenAPI();
        paths = new Paths();
        openAPI.setPaths(paths);
        
        pathItem = new PathItem();
        paths.addPathItem("/test", pathItem);
        
        postOperation = new Operation().operationId("testPost");
        putOperation = new Operation().operationId("testPut");
        patchOperation = new Operation().operationId("testPatch");
        getOperation = new Operation().operationId("testGet");
        
        pathItem.setPost(postOperation);
        pathItem.setPut(putOperation);
        pathItem.setPatch(patchOperation);
        pathItem.setGet(getOperation);
    }

    @Test
    void shouldAddIdempotencyKeyHeaderToPostPutPatchOperations() {
        // Act
        customizer.customise(openAPI);
        
        // Assert
        // POST operation should have Idempotency-Key header
        List<Parameter> postParams = postOperation.getParameters();
        assertNotNull(postParams);
        assertTrue(postParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
        
        // PUT operation should have Idempotency-Key header
        List<Parameter> putParams = putOperation.getParameters();
        assertNotNull(putParams);
        assertTrue(putParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
        
        // PATCH operation should have Idempotency-Key header
        List<Parameter> patchParams = patchOperation.getParameters();
        assertNotNull(patchParams);
        assertTrue(patchParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
        
        // GET operation should NOT have Idempotency-Key header
        List<Parameter> getParams = getOperation.getParameters();
        if (getParams != null) {
            assertFalse(getParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
        }
    }

    @Test
    void shouldNotAddIdempotencyKeyHeaderToOperationsWithDisableIdempotencyExtension() {
        // Arrange
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(IdempotencyOperationCustomizer.DISABLE_IDEMPOTENCY_EXTENSION, true);
        postOperation.setExtensions(extensions);
        
        // Act
        customizer.customise(openAPI);
        
        // Assert
        // POST operation should NOT have Idempotency-Key header because it has the disable extension
        List<Parameter> postParams = postOperation.getParameters();
        if (postParams != null) {
            assertFalse(postParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
        }
        
        // PUT operation should have Idempotency-Key header
        List<Parameter> putParams = putOperation.getParameters();
        assertNotNull(putParams);
        assertTrue(putParams.stream().anyMatch(p -> "Idempotency-Key".equals(p.getName()) && "header".equals(p.getIn())));
    }

    @Test
    void shouldNotDuplicateIdempotencyKeyHeaderIfAlreadyPresent() {
        // Arrange
        Parameter existingParam = new Parameter()
                .name("Idempotency-Key")
                .in("header")
                .description("Existing description");
        postOperation.setParameters(Collections.singletonList(existingParam));
        
        // Act
        customizer.customise(openAPI);
        
        // Assert
        // POST operation should still have only one Idempotency-Key header
        List<Parameter> postParams = postOperation.getParameters();
        assertNotNull(postParams);
        assertEquals(1, postParams.size());
        assertEquals("Idempotency-Key", postParams.get(0).getName());
        assertEquals("header", postParams.get(0).getIn());
        assertEquals("Existing description", postParams.get(0).getDescription());
    }
}