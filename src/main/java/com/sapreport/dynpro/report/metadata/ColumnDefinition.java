package com.sapreport.dynpro.report.metadata;

import java.util.Map;

/**
 * One output column of a report result grid. {@code columnType} defaults to
 * {@link ColumnType#DATA} when absent (plain data columns are the vast
 * majority and never need to set it). {@code editable}/{@code editValidation}
 * apply to data columns; {@code action} only to {@link ColumnType#ACTION}
 * columns; {@code displayHints} only to {@link ColumnType#STATUS} columns.
 */
public record ColumnDefinition(
        String field,
        String label,
        String dataType,
        String format,
        String align,
        boolean sortable,
        String aggregate,
        ColumnType columnType,
        boolean editable,
        EditValidation editValidation,
        ColumnAction action,
        Map<String, DisplayHint> displayHints
) {
    public ColumnDefinition {
        columnType = columnType == null ? ColumnType.DATA : columnType;
        displayHints = displayHints == null ? Map.of() : Map.copyOf(displayHints);
    }

    /** Pre-actions/editing constructor, kept so existing metadata/tests need no changes. */
    public ColumnDefinition(String field, String label, String dataType, String format, String align,
                             boolean sortable, String aggregate) {
        this(field, label, dataType, format, align, sortable, aggregate, ColumnType.DATA, false, null, null, Map.of());
    }
}
