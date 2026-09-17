package com.sapreport.dynpro.report.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of {@link ReportDataResponse#effectiveColumns()} — the column's
 * field name plus whether it should be shown by default in a column picker.
 */
public record EffectiveColumn(String columnName, @JsonProperty("isDefault") boolean isDefault) {
}
