package com.sapreport.dynpro.report.lookup;

/**
 * Source of F4 value-help results for one {@code LOOKUP} parameter of one
 * report. Also the single source of truth a submitted parameter value is
 * re-validated against server-side (see {@code ReportParameterValidator}) —
 * the client's submitted label is never trusted, only the value, and only
 * once confirmed to still exist here.
 */
public interface ReportLookupProvider {

    boolean supports(String reportCode, String parameterName);

    LookupResponse search(String reportCode, String parameterName, String query, int page, int pageSize);

    default boolean isValidValue(String reportCode, String parameterName, String value) {
        if (value == null) {
            return false;
        }
        return search(reportCode, parameterName, value, 1, Integer.MAX_VALUE).items().stream()
                .anyMatch(item -> value.equals(item.value()));
    }
}
