package com.sapreport.dynpro.report.query;

import java.util.List;
import java.util.Map;

/**
 * Everything a {@link ReportQueryExecutor} needs to run one report: the
 * already-validated/coerced parameter values, which columns are currently
 * visible (so the executor need not select more than it must), sort, and
 * paging.
 */
public record ReportQueryRequest(
        String reportCode,
        Map<String, Object> parameters,
        List<String> effectiveColumns,
        List<SortSpec> sort,
        int page,
        int pageSize
) {
}
