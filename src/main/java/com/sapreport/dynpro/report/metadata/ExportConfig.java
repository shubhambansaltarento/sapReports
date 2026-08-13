package com.sapreport.dynpro.report.metadata;

import java.util.List;

/**
 * Export formats a report supports. Metadata only in this iteration — no
 * export-generation endpoint exists yet, see reports-overview.md.
 */
public record ExportConfig(List<String> formats) {
}
