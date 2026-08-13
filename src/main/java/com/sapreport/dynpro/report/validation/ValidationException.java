package com.sapreport.dynpro.report.validation;

import java.util.List;

/**
 * Thrown with every validation failure collected (not fail-fast), so a
 * single response can report every broken field at once.
 */
public class ValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super("Validation failed: " + errors.size() + " error(s)");
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
