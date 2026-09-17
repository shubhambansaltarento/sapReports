package com.sapreport.dynpro.report.partspackinglist;

/** Wraps a failure running the parts packing list query against Databricks. */
public class PartsPackingListDatabricksQueryException extends RuntimeException {

    public PartsPackingListDatabricksQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
