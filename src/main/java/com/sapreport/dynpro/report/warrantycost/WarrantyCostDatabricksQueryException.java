package com.sapreport.dynpro.report.warrantycost;

/** Wraps a failure running the warranty cost query against Databricks. */
public class WarrantyCostDatabricksQueryException extends RuntimeException {

    public WarrantyCostDatabricksQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
