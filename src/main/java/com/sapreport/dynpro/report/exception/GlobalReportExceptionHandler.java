package com.sapreport.dynpro.report.exception;

import com.sapreport.dynpro.report.api.ErrorResponse;
import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single source of every error response the report endpoints produce — no
 * per-controller try/catch. Maps each failure kind to the status codes the
 * spec defines and the shared {@link ErrorResponse} envelope.
 */
@RestControllerAdvice(basePackages = "com.sapreport.dynpro.report.api")
public class GlobalReportExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalReportExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", ex.errors(), request);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReportNotFoundException ex, HttpServletRequest request) {
        List<ValidationError> errors = List.of(new ValidationError("reportCode", "NOT_FOUND", ex.getMessage()));
        return respond(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", errors, request);
    }

    @ExceptionHandler(ConfigVersionMismatchException.class)
    public ResponseEntity<ErrorResponse> handleConfigVersionMismatch(ConfigVersionMismatchException ex,
                                                                      HttpServletRequest request) {
        List<ValidationError> errors = List.of(new ValidationError("configVersion", "CONFIG_VERSION_MISMATCH",
                ex.getMessage(), Map.of("expected", ex.expected(), "submitted", ex.submitted())));
        return respond(HttpStatus.CONFLICT, "CONFIG_VERSION_MISMATCH", errors, request);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex, HttpServletRequest request) {
        List<ValidationError> errors = List.of(new ValidationError("documentKey", "NOT_FOUND", ex.getMessage()));
        return respond(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", errors, request);
    }

    @ExceptionHandler(ReportAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ReportAccessDeniedException ex, HttpServletRequest request) {
        List<ValidationError> errors = List.of(new ValidationError("companyCode", "FORBIDDEN", ex.getMessage()));
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex,
                                                                 HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        List<ValidationError> errors = List.of(new ValidationError("", "UNEXPECTED_ERROR", ex.toString()));
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", errors, request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, List<ValidationError> errors,
                                                    HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        return ResponseEntity.status(status).body(new ErrorResponse(traceId, code, errors));
    }

    private String resolveTraceId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-Id");
        return (correlationId == null || correlationId.isBlank()) ? UUID.randomUUID().toString() : correlationId;
    }
}
