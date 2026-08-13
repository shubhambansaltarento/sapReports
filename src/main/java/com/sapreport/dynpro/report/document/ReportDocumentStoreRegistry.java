package com.sapreport.dynpro.report.document;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Resolves the {@link ReportDocumentStore} for a given report — one registry, no per-report if/else in callers. */
@Component
public class ReportDocumentStoreRegistry {

    private final List<ReportDocumentStore> stores;

    public ReportDocumentStoreRegistry(List<ReportDocumentStore> stores) {
        this.stores = List.copyOf(stores);
    }

    public Optional<ReportDocumentStore> find(String reportCode) {
        return stores.stream()
                .filter(store -> store.supports(reportCode))
                .findFirst();
    }
}
