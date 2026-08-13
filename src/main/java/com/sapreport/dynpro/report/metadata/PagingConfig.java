package com.sapreport.dynpro.report.metadata;

/** Default and maximum page size a report's data endpoint will honor. */
public record PagingConfig(int defaultPageSize, int maxPageSize) {
}
