package com.sapreport.dynpro.report.metadata;

/**
 * Date-range validation rules for a parameter. {@code minDate}/{@code maxDate}
 * are either an ISO date literal or the token {@code $TODAY}, resolved
 * server-side at validation time.
 */
public record ValidationRules(
        String minDate,
        String maxDate,
        Integer maxRangeDays,
        Boolean requireBothBounds
) {
}
