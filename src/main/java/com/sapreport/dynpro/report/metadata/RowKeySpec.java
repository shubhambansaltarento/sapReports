package com.sapreport.dynpro.report.metadata;

import java.util.List;

/**
 * Declares which row fields make up a report's row identity. When present,
 * every row in the data response gets a computed {@code _rowKey} (those
 * fields joined by {@code separator}) and {@code _rowVersion}, both required
 * on any action targeting that row.
 */
public record RowKeySpec(List<String> fields, String separator) {
    public RowKeySpec {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
