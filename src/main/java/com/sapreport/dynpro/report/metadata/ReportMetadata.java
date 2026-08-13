package com.sapreport.dynpro.report.metadata;

import java.util.List;

/**
 * Full metadata definition for one report: its parameters, its column
 * groups (with visibility conditions), export/paging capabilities, and the
 * {@code configVersion} the data endpoint checks against.
 *
 * <p>{@code reportGroup}/{@code groupLabel}/{@code groupOrder} let sibling
 * reports render as tabs of one group while remaining independent report
 * codes. {@code parameterGroups} are cross-parameter constraints (e.g.
 * "exactly one of these checkboxes"). {@code dateRangeConstraints} are the
 * equivalent cross-field check for two independently-named {@code DATE}
 * parameters. {@code rowKey} — when present — turns on {@code _rowKey}/
 * {@code _rowVersion} computation for every data row, and {@code actions}
 * lists the row/bulk actions this report exposes.
 */
public record ReportMetadata(
        String reportCode,
        String title,
        String configVersion,
        List<ParameterDefinition> parameters,
        List<ColumnGroup> columnGroups,
        ExportConfig export,
        PagingConfig paging,
        String reportGroup,
        String groupLabel,
        Integer groupOrder,
        List<ParameterGroup> parameterGroups,
        RowKeySpec rowKey,
        List<ActionDefinition> actions,
        List<DateRangeConstraint> dateRangeConstraints
) {
    public ReportMetadata {
        parameterGroups = parameterGroups == null ? List.of() : List.copyOf(parameterGroups);
        actions = actions == null ? List.of() : List.copyOf(actions);
        dateRangeConstraints = dateRangeConstraints == null ? List.of() : List.copyOf(dateRangeConstraints);
    }

    /** Pre-actions/grouping constructor, kept so existing metadata/tests need no changes. */
    public ReportMetadata(String reportCode, String title, String configVersion, List<ParameterDefinition> parameters,
                           List<ColumnGroup> columnGroups, ExportConfig export, PagingConfig paging) {
        this(reportCode, title, configVersion, parameters, columnGroups, export, paging,
                null, null, null, List.of(), null, List.of(), List.of());
    }

    /** Pre-dateRangeConstraints constructor, kept so existing metadata/tests need no changes. */
    public ReportMetadata(String reportCode, String title, String configVersion, List<ParameterDefinition> parameters,
                           List<ColumnGroup> columnGroups, ExportConfig export, PagingConfig paging,
                           String reportGroup, String groupLabel, Integer groupOrder,
                           List<ParameterGroup> parameterGroups, RowKeySpec rowKey, List<ActionDefinition> actions) {
        this(reportCode, title, configVersion, parameters, columnGroups, export, paging,
                reportGroup, groupLabel, groupOrder, parameterGroups, rowKey, actions, List.of());
    }
}
