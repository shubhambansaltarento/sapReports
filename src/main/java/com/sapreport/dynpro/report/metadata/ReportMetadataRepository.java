package com.sapreport.dynpro.report.metadata;

import java.util.Optional;

/**
 * Source of per-report metadata (parameters, column groups, config version).
 * Backed by classpath JSON resources for now; swappable for a
 * database-backed implementation later (see jdbc-connection.md §3.2)
 * without any caller needing to change.
 */
public interface ReportMetadataRepository {

    Optional<ReportMetadata> findByReportCode(String reportCode);
}
