package com.sapreport.dynpro.report.metadata;

/**
 * Cross-field range check between two independently-named {@code DATE}
 * parameters (as opposed to a single {@link ControlType#DATE_RANGE}
 * parameter's own {@link ValidationRules}) — e.g. {@code dateFrom}/
 * {@code dateTo} on a screen that models them as two separate fields rather
 * than one {from,to} control.
 */
public record DateRangeConstraint(String fromField, String toField, Integer maxRangeDays, Boolean requireBothBounds) {
}
