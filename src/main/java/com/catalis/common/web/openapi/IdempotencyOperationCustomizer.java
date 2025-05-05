package com.catalis.common.web.openapi;

import com.catalis.common.web.idempotency.annotation.DisableIdempotency;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * Operation customizer that marks operations with the DisableIdempotency annotation.
 * This customizer adds an extension to the OpenAPI operation that can be used by
 * the IdempotencyOpenAPICustomizer to exclude these operations from idempotency.
 */
@Component
public class IdempotencyOperationCustomizer implements OperationCustomizer {

    public static final String DISABLE_IDEMPOTENCY_EXTENSION = "x-disable-idempotency";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.hasMethodAnnotation(DisableIdempotency.class)) {
            // Mark this operation as having the DisableIdempotency annotation
            operation.addExtension(DISABLE_IDEMPOTENCY_EXTENSION, true);
        }
        return operation;
    }
}