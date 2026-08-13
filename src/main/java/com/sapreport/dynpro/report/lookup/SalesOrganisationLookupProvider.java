package com.sapreport.dynpro.report.lookup;

import com.sapreport.dynpro.report.security.ReportCallerContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Placeholder {@link ReportLookupProvider} for {@code WARRANTY_LABOUR_REPORT}'s
 * {@code salesOrganisation} F4 value-help — canned in-memory options, same
 * "exercisable end-to-end before the real integration exists" role as
 * {@code StubReportQueryExecutor}. Results are filtered to the caller's
 * {@link ReportCallerContext#authorizedSalesOrganisations()} (confirmed
 * requirement) — never returns an org outside the caller's scope, regardless
 * of query text.
 */
@Component
public class SalesOrganisationLookupProvider implements ReportLookupProvider {

    static final String REPORT_CODE = "WARRANTY_LABOUR_REPORT";
    static final String PARAMETER_NAME = "salesOrganisation";

    private static final List<LookupItem> ALL_ORGANISATIONS = List.of(
            new LookupItem("TVSL01", "TVS Lucas - Sales Org 01"),
            new LookupItem("TVSL02", "TVS Lucas - Sales Org 02"),
            new LookupItem("TVSM01", "TVS Motor - Sales Org 01"));

    private final ReportCallerContext callerContext;

    public SalesOrganisationLookupProvider(ReportCallerContext callerContext) {
        this.callerContext = callerContext;
    }

    @Override
    public boolean supports(String reportCode, String parameterName) {
        return REPORT_CODE.equals(reportCode) && PARAMETER_NAME.equals(parameterName);
    }

    @Override
    public LookupResponse search(String reportCode, String parameterName, String query, int page, int pageSize) {
        Set<String> authorized = callerContext.authorizedSalesOrganisations();
        String normalizedQuery = query == null ? "" : query.toLowerCase();

        List<LookupItem> matches = ALL_ORGANISATIONS.stream()
                .filter(item -> authorized.contains(item.value()))
                .filter(item -> normalizedQuery.isBlank()
                        || item.value().toLowerCase().contains(normalizedQuery)
                        || item.label().toLowerCase().contains(normalizedQuery))
                .toList();

        int effectivePage = Math.max(page, 1);
        int fromIndex = Math.min((effectivePage - 1) * pageSize, matches.size());
        int toIndex = Math.min(fromIndex + pageSize, matches.size());

        return new LookupResponse(matches.subList(fromIndex, toIndex), effectivePage, pageSize, matches.size());
    }
}
