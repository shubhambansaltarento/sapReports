package com.sapreport.dynpro.report.query;

/** Wraps a failure running the PARTS_PACKING_LIST query against Databricks. */
public class PartsPackingListQueryException extends RuntimeException {

    public PartsPackingListQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
