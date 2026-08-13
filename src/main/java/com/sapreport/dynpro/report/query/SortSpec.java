package com.sapreport.dynpro.report.query;

/** One sort instruction from the client: a column field and direction. */
public record SortSpec(String field, String dir) {
}
