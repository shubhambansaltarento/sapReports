package com.sapreport.dynpro.report.document;

import com.sapreport.dynpro.report.exception.DocumentNotFoundException;
import com.sapreport.dynpro.report.exception.ReportNotFoundException;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import org.springframework.stereotype.Service;

/** Backs {@code GET /api/v1/reports/{reportCode}/documents/{documentKey}}. */
@Service
public class ReportDocumentService {

    private final ReportMetadataRepository metadataRepository;
    private final ReportDocumentStoreRegistry storeRegistry;

    public ReportDocumentService(ReportMetadataRepository metadataRepository, ReportDocumentStoreRegistry storeRegistry) {
        this.metadataRepository = metadataRepository;
        this.storeRegistry = storeRegistry;
    }

    public DocumentContent fetch(String reportCode, String documentKey) {
        metadataRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new ReportNotFoundException(reportCode));

        ReportDocumentStore store = storeRegistry.find(reportCode)
                .orElseThrow(() -> new IllegalStateException("No ReportDocumentStore registered for " + reportCode));

        return store.fetch(reportCode, documentKey)
                .orElseThrow(() -> new DocumentNotFoundException(documentKey));
    }
}
