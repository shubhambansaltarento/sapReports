package com.sapreport.dynpro.report.dealerledger;

/** Wraps a failure running the dealer ledger query against Databricks. */
public class DealerLedgerDatabricksQueryException extends RuntimeException {

    public DealerLedgerDatabricksQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
