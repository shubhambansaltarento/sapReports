package com.sapreport.dynpro.report.query;

/**
 * Port to the underlying data source that actually runs a report's query
 * (Databricks SQL Warehouse via JDBC, per the spec) and returns its rows.
 * Deliberately just an interface here — no driver/connection logic — so the
 * generic report engine can be built and tested against a stub while the
 * real implementation (see jdbc-connection.md) is wired in separately.
 */
public interface ReportQueryExecutor {

    ReportQueryResult execute(ReportQueryRequest request);
}
