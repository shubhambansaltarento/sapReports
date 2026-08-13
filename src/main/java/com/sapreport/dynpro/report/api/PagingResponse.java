package com.sapreport.dynpro.report.api;

/** Actual paging applied to a data response, after clamping to the report's {@code maxPageSize}. */
public record PagingResponse(int page, int pageSize, long totalRows, int totalPages) {
}
