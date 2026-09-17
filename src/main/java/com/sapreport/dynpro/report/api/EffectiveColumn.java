package com.sapreport.dynpro.report.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry of {@link ReportDataResponse#effectiveColumns()} — the column's
 * field name, whether it should be shown by default in a column picker
 * ({@code isDefault}), and whether it should ever show in the grid at all
 * ({@code isVisible} — a master switch, e.g. for conditional detail columns).
 */
public record EffectiveColumn(String columnName, @JsonProperty("isDefault") boolean isDefault,
                               @JsonProperty("isVisible") boolean isVisible) {
}
