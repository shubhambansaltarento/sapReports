package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.condition.Condition;

import java.util.List;

/**
 * A group of columns that appear together. {@code visibleWhen} is null for
 * always-visible groups (e.g. the base ledger columns), or a structured
 * {@link Condition} evaluated against submitted parameters to decide whether
 * this group's columns are included in {@code effectiveColumns}.
 */
public record ColumnGroup(
        String key,
        String label,
        Condition visibleWhen,
        List<ColumnDefinition> columns
) {
}
