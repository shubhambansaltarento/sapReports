package com.sapreport.dynpro.report.document;

import java.util.Optional;

/**
 * Source of a report's documents (e.g. a generated invoice PDF).
 * {@code documentKey} is opaque and server-issued elsewhere (never a
 * client-constructed path) — this port only resolves an already-issued key
 * back to its bytes.
 */
public interface ReportDocumentStore {

    boolean supports(String reportCode);

    Optional<DocumentContent> fetch(String reportCode, String documentKey);
}
