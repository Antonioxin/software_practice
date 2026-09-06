package com.wemove.identity.api;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<FieldViolation> errors;

    public ApiException(HttpStatus status, String code, String detail) {
        this(status, code, detail, List.of());
    }

    public ApiException(HttpStatus status, String code, String detail, List<FieldViolation> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public List<FieldViolation> getErrors() { return errors; }

    public record FieldViolation(String field, String code, String message) {}
}
