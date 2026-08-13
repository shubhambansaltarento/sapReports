package com.sapreport.dynpro.report.lookup;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the {@link ReportLookupProvider} for a given report/parameter —
 * one registry, no per-report if/else in callers. Empty when no providers
 * are registered yet, which is the case for every report today.
 */
@Component
public class ReportLookupProviderRegistry {

    private final List<ReportLookupProvider> providers;

    public ReportLookupProviderRegistry(List<ReportLookupProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public Optional<ReportLookupProvider> find(String reportCode, String parameterName) {
        return providers.stream()
                .filter(provider -> provider.supports(reportCode, parameterName))
                .findFirst();
    }
}
