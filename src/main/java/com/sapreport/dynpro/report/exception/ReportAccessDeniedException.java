package com.sapreport.dynpro.report.exception;

/** Thrown when the requested {@code companyCode} is outside the caller's authorization scope. */
public class ReportAccessDeniedException extends RuntimeException {

    public ReportAccessDeniedException(String companyCode) {
        super("Company code '" + companyCode + "' is outside the caller's authorization scope");
    }
}
