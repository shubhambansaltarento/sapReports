package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.metadata.ActionDefinition;
import com.sapreport.dynpro.report.metadata.ColumnGroup;
import com.sapreport.dynpro.report.metadata.DateRangeConstraint;
import com.sapreport.dynpro.report.metadata.ExportConfig;
import com.sapreport.dynpro.report.metadata.PagingConfig;
import com.sapreport.dynpro.report.metadata.ParameterDefinition;
import com.sapreport.dynpro.report.metadata.ParameterGroup;
import com.sapreport.dynpro.report.metadata.RowKeySpec;

import java.util.List;

/**
 * Response body of {@code GET /api/v1/reports/{reportCode}/config}.
 * {@code parameterGroups}/{@code dateRangeConstraints}/{@code actions}/
 * {@code rowKey}/grouping fields are what the actions-extension UI reads to
 * render cross-field constraints, action buttons, and report tabs — the
 * whole point of declaring them in metadata is for the client to get them
 * from here, not to hardcode them.
 */
public record ReportConfigResponse(
        String reportCode,
        String title,
        String configVersion,
        ReportContext context,
        List<ParameterDefinition> parameters,
        List<ColumnGroup> columnGroups,
        ExportConfig export,
        PagingConfig paging,
        String reportGroup,
        String groupLabel,
        Integer groupOrder,
        List<ParameterGroup> parameterGroups,
        List<DateRangeConstraint> dateRangeConstraints,
        RowKeySpec rowKey,
        List<ActionDefinition> actions
) {
}
