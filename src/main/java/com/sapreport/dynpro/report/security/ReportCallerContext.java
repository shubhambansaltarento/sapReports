package com.sapreport.dynpro.report.security;

import java.util.Set;

/**
 * The authenticated caller's identity as far as reports are concerned:
 * dealer identity (echoed in the config response's {@code context}, never
 * accepted as a request parameter) and which company codes they're
 * authorized against. Backed by a fixed stub until JWT authentication
 * (jwt-auth.md) is implemented — real implementations will read this off
 * the validated JWT's claims instead.
 */
public interface ReportCallerContext {

    String dealerCode();

    String dealerDescription();

    Set<String> authorizedCompanyCodes();

    /**
     * Sales organisation codes this caller may search/select — SAP treats
     * sales organisation as distinct from company code, so this isn't just
     * an alias for {@link #authorizedCompanyCodes()}. Defaults to empty
     * (no access) so every existing caller-context implementation keeps
     * compiling; override once this scoping is actually needed.
     */
    default Set<String> authorizedSalesOrganisations() {
        return Set.of();
    }
}
