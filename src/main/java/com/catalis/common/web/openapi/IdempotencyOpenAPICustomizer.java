package com.catalis.common.web.openapi;

import com.catalis.common.web.idempotency.annotation.DisableIdempotency;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * OpenAPI customizer that adds the X-Idempotency-Key header to POST, PUT, and PATCH operations.
 * It excludes operations that have the {@link DisableIdempotency} annotation.
 */
@Component
public class IdempotencyOpenAPICustomizer implements OpenApiCustomizer {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final List<PathItem.HttpMethod> IDEMPOTENT_METHODS = Arrays.asList(
            PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH);

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) -> {
            // Process POST operations
            processOperation(pathItem.getPost(), PathItem.HttpMethod.POST);

            // Process PUT operations
            processOperation(pathItem.getPut(), PathItem.HttpMethod.PUT);

            // Process PATCH operations
            processOperation(pathItem.getPatch(), PathItem.HttpMethod.PATCH);
        });
    }

    private void processOperation(Operation operation, PathItem.HttpMethod method) {
        if (operation == null || !IDEMPOTENT_METHODS.contains(method)) {
            return;
        }

        // Skip if the operation has the DisableIdempotency annotation
        if (hasDisableIdempotencyExtension(operation)) {
            return;
        }

        // Check if the X-Idempotency-Key parameter already exists
        Optional<Parameter> existingParam = operation.getParameters() != null ?
                operation.getParameters().stream()
                        .filter(p -> IDEMPOTENCY_KEY_HEADER.equals(p.getName()) && "header".equals(p.getIn()))
                        .findFirst() :
                Optional.empty();

        // Add the parameter if it doesn't exist
        if (existingParam.isEmpty()) {
            Parameter idempotencyKeyParam = new Parameter()
                    .name(IDEMPOTENCY_KEY_HEADER)
                    .in("header")
                    .description("Unique key for idempotent requests. If provided, ensures that identical requests with the same key will only be processed once.")
                    .required(false)
                    .schema(new io.swagger.v3.oas.models.media.StringSchema());

            if (operation.getParameters() == null) {
                operation.setParameters(List.of(idempotencyKeyParam));
            } else {
                operation.addParametersItem(idempotencyKeyParam);
            }
        }
    }

    private boolean hasDisableIdempotencyExtension(Operation operation) {
        // Check if the operation has the x-disable-idempotency extension
        Object extension = operation.getExtensions() != null ? 
                operation.getExtensions().get(IdempotencyOperationCustomizer.DISABLE_IDEMPOTENCY_EXTENSION) : null;
        return Boolean.TRUE.equals(extension);
    }
}
