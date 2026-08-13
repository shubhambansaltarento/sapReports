package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.validation.ValidationError;

import java.util.List;

/** Standard error envelope for every report endpoint failure. */
public record ErrorResponse(String traceId, String code, List<ValidationError> errors) {
}
