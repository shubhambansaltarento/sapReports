package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;

import java.util.List;

/** One row's result within an {@link ActionResponse}. {@code rowVersion} is the new (or current) version; null on SKIPPED/ROW_NOT_FOUND. */
public record RowActionResult(String rowKey, RowActionStatus status, String rowVersion, List<ValidationError> errors) {
    public RowActionResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
