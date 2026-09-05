package com.wemove.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem = base(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
        if (!ex.getErrors().isEmpty()) problem.setProperty("errors", ex.getErrors());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS) response.header(HttpHeaders.RETRY_AFTER, "600");
        return response.body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(base(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "请求内容无法解析，或包含未定义字段。", request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream().map(this::fieldError).toList();
        ProblemDetail problem = base(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.unprocessableEntity().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(base(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", "该记录已存在，请刷新后重试。", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        log.error("Unhandled request failure, requestId={}", requestId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(base(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务暂时不可用，请稍后重试。", request));
    }

    private ProblemDetail base(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("requestId", request.getAttribute("requestId"));
        return problem;
    }

    private Map<String, String> fieldError(FieldError error) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("field", error.getField());
        result.put("code", error.getCode() == null ? "INVALID" : error.getCode());
        result.put("message", error.getDefaultMessage() == null ? "字段不合法" : error.getDefaultMessage());
        return result;
    }
}
