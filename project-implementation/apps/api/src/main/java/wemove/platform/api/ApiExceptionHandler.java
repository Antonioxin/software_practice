package wemove.platform.api;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem = base(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
        if (!ex.getErrors().isEmpty()) problem.setProperty("errors", ex.getErrors());
        ResponseEntity.BodyBuilder response =
                ResponseEntity.status(ex.getStatus())
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS)
            response.header(HttpHeaders.RETRY_AFTER, "600");
        if (ex.getCode().equals("REQUEST_IN_PROGRESS"))
            response.header(HttpHeaders.RETRY_AFTER, "2");
        return response.body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.JsonMappingException mapping) {
            String field =
                    mapping.getPath().stream()
                            .map(
                                    p ->
                                            p.getFieldName() == null
                                                    ? "[" + p.getIndex() + "]"
                                                    : p.getFieldName())
                            .collect(java.util.stream.Collectors.joining("."));
            if (mapping
                    instanceof
                    com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException unknown)
                field = unknown.getPropertyName();
            ProblemDetail problem =
                    base(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "VALIDATION_ERROR",
                            "字段类型或名称不符合接口定义。",
                            request);
            problem.setProperty(
                    "errors",
                    List.of(
                            Map.of(
                                    "field",
                                    field,
                                    "code",
                                    "INVALID_TYPE",
                                    "message",
                                    "请检查字段名称及类型。")));
            return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem);
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(
                        base(
                                HttpStatus.BAD_REQUEST,
                                "MALFORMED_REQUEST",
                                "请求内容无法解析，或包含未定义字段。",
                                request));
    }

    @ExceptionHandler({
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.bind.MissingRequestHeaderException.class
    })
    ResponseEntity<ProblemDetail> handleParameter(Exception ex, HttpServletRequest request) {
        ProblemDetail problem =
                base(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "必填参数缺失或类型无效。", request);
        problem.setProperty(
                "errors",
                List.of(
                        Map.of(
                                "field",
                                "parameter",
                                "code",
                                "INVALID_VALUE",
                                "message",
                                "请检查路径、查询参数及请求头。")));
        return ResponseEntity.unprocessableEntity()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream().map(this::fieldError).toList();
        ProblemDetail problem =
                base(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "请检查标记的字段。", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.unprocessableEntity()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleConflict(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(base(HttpStatus.CONFLICT, "UNIQUE_CONFLICT", "该记录已存在，请刷新后重试。", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        log.error("Unhandled request failure, requestId={}", requestId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(
                        base(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_ERROR",
                                "服务暂时不可用，请稍后重试。",
                                request));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMediaType(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(
                        base(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "UNSUPPORTED_MEDIA_TYPE",
                                "请使用 JSON 请求内容。",
                                request));
    }

    private ProblemDetail base(
            HttpStatus status, String code, String detail, HttpServletRequest request) {
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
        result.put(
                "message", error.getDefaultMessage() == null ? "字段不合法" : error.getDefaultMessage());
        return result;
    }
}
