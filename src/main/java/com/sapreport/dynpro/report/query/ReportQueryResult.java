package com.sapreport.dynpro.report.query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Raw result of running one report query, before response assembly. */
public record ReportQueryResult(
        List<Map<String, Object>> rows,
        Map<String, Object> totals,
        long totalRows,
        Instant dataAsOf,
        long queryMs
) {
}
