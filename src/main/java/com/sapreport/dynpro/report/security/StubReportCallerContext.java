package com.sapreport.dynpro.report.security;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * TEMPORARY stand-in for {@link ReportCallerContext} used until JWT
 * authentication (jwt-auth.md) is wired in. Returns a fixed development
 * identity so the report endpoints are exercisable end-to-end today;
 * replace with a bean that reads dealerCode/companyCodes off the validated
 * JWT's claims once that lands.
 */
@Component
public class StubReportCallerContext implements ReportCallerContext {

    @Override
    public String dealerCode() {
        return "1130";
    }

    @Override
    public String dealerDescription() {
        return "PAWAN SARKAR AUTOMOBILES";
    }

    @Override
    public Set<String> authorizedCompanyCodes() {
        return Set.of("TVSL");
    }

    @Override
    public Set<String> authorizedSalesOrganisations() {
        return Set.of("TVSL01");
    }
}
