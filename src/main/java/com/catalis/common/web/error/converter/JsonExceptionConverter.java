package com.catalis.common.web.error.converter;

import com.catalis.common.web.error.exceptions.BusinessException;
import com.catalis.common.web.error.exceptions.InvalidRequestException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.stereotype.Component;

/**
 * Converter for JSON processing exceptions.
 * Converts JSON exceptions to InvalidRequestException.
 */
@Component
public class JsonExceptionConverter implements ExceptionConverter<JsonProcessingException> {

    /**
     * Creates a new JsonExceptionConverter.
     */
    public JsonExceptionConverter() {
        // Default constructor
    }

    @Override
    public Class<JsonProcessingException> getExceptionType() {
        return JsonProcessingException.class;
    }

    @Override
    public BusinessException convert(JsonProcessingException exception) {
        if (exception instanceof JsonParseException) {
            return InvalidRequestException.withReason("JSON_PARSE_ERROR", "Invalid JSON format: " + exception.getMessage());
        } else if (exception instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException ex = (UnrecognizedPropertyException) exception;
            return InvalidRequestException.forField(ex.getPropertyName(), "unknown-property", "Unrecognized field");
        } else if (exception instanceof InvalidFormatException) {
            InvalidFormatException ex = (InvalidFormatException) exception;
            String fieldName = ex.getPath().isEmpty() ? "unknown" : ex.getPath().get(0).getFieldName();
            return InvalidRequestException.forField(fieldName, ex.getValue().toString(), "Invalid format");
        } else if (exception instanceof MismatchedInputException) {
            MismatchedInputException ex = (MismatchedInputException) exception;
            String fieldName = ex.getPath().isEmpty() ? "unknown" : ex.getPath().get(0).getFieldName();
            return InvalidRequestException.forField(fieldName, "invalid-value", "Mismatched input");
        } else if (exception instanceof JsonMappingException) {
            JsonMappingException ex = (JsonMappingException) exception;
            String fieldName = ex.getPath().isEmpty() ? "unknown" : ex.getPath().get(0).getFieldName();
            return InvalidRequestException.forField(fieldName, "mapping-error", "JSON mapping error");
        }

        // Fallback
        return InvalidRequestException.withReason("JSON_ERROR", "JSON processing error: " + exception.getMessage());
    }
}
