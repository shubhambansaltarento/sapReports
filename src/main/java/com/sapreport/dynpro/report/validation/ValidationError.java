package com.sapreport.dynpro.report.validation;

import java.util.Map;

/**
 * One field-level validation failure. {@code field} uses a dotted path
 * (e.g. {@code "postingDate.to"}) so the Angular reactive form can map it
 * straight to a control.
 */
public record ValidationError(String field, String code, String message, Map<String, Object> params) {

    public ValidationError(String field, String code, String message) {
        this(field, code, message, Map.of());
    }
}
