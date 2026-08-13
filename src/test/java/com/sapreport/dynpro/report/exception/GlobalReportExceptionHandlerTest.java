package com.sapreport.dynpro.report.exception;

import com.sapreport.dynpro.report.api.ErrorResponse;
import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import com.sapreport.dynpro.report.validation.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalReportExceptionHandlerTest {

    private final GlobalReportExceptionHandler handler = new GlobalReportExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void validationException_mapsTo422() {
        ValidationException ex = new ValidationException(
                List.of(new ValidationError("companyCode", ValidationErrorCodes.REQUIRED, "required")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().errors()).hasSize(1);
    }

    @Test
    void reportNotFound_mapsTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ReportNotFoundException("BOGUS"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("REPORT_NOT_FOUND");
    }

    @Test
    void configVersionMismatch_mapsTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleConfigVersionMismatch(
                new ConfigVersionMismatchException("2026.08.1", "old"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONFIG_VERSION_MISMATCH");
    }

    @Test
    void accessDenied_mapsTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new ReportAccessDeniedException("OTHERCO"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void malformedJson_mapsTo400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        ResponseEntity<ErrorResponse> response = handler.handleMalformedRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void traceId_usesIncomingCorrelationIdWhenPresent() {
        when(request.getHeader("X-Correlation-Id")).thenReturn("abc-123");

        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ReportNotFoundException("BOGUS"), request);

        assertThat(response.getBody().traceId()).isEqualTo("abc-123");
    }

    @Test
    void traceId_generatedWhenAbsent() {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ReportNotFoundException("BOGUS"), request);

        assertThat(response.getBody().traceId()).isNotBlank();
    }
}
