package com.sapreport.dynpro.report.metadata;

/** F4-style value-help wiring for a {@link ControlType#LOOKUP} parameter. */
public record LookupConfig(
        String endpoint,
        Integer minChars,
        Integer pageSize,
        String valueField,
        String labelField
) {
}
