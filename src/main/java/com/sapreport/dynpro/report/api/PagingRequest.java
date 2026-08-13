package com.sapreport.dynpro.report.api;

/** Requested page/pageSize; either may be omitted, in which case the report's defaults apply. */
public record PagingRequest(Integer page, Integer pageSize) {
}
