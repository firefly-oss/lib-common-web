package com.catalis.common.web.error.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd/MM/yyyy'T'HH:mm:ss.SSSSSS",
            timezone = "Europe/Madrid"
    )
    private LocalDateTime timestamp;

    private String path;
    private Integer status;
    private String error;
    private String message;
    private String traceId;
    private List<ValidationError> errors;

    @Data
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}

