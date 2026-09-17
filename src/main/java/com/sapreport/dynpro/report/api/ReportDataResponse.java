package com.sapreport.dynpro.report.api;

import java.util.List;
import java.util.Map;

/** Response body of {@code POST /api/v1/reports/{reportCode}/data}. */
public record ReportDataResponse(
        String reportCode,
        String configVersion,
        List<EffectiveColumn> effectiveColumns,
        List<Map<String, Object>> rows,
        Map<String, Object> totals,
        PagingResponse paging,
        ResponseMeta meta
) {
}
