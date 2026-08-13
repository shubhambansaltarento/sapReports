package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.query.SortSpec;

import java.util.List;
import java.util.Map;

/** Request body of {@code POST /api/v1/reports/{reportCode}/data}. */
public record ReportDataRequest(
        Map<String, Object> parameters,
        PagingRequest paging,
        List<SortSpec> sort,
        String configVersion
) {
}
