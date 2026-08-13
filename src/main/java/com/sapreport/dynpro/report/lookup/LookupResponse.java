package com.sapreport.dynpro.report.lookup;

import java.util.List;

/** Response body of {@code GET /api/v1/reports/{reportCode}/lookups/{parameterName}}. */
public record LookupResponse(List<LookupItem> items, int page, int pageSize, long totalItems) {
}
