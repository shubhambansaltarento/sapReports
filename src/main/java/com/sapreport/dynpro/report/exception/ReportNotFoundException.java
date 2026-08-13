package com.sapreport.dynpro.report.exception;

/** Thrown when {@code reportCode} doesn't match any known report metadata. */
public class ReportNotFoundException extends RuntimeException {

    private final String reportCode;

    public ReportNotFoundException(String reportCode) {
        super("Unknown report code: " + reportCode);
        this.reportCode = reportCode;
    }

    public String reportCode() {
        return reportCode;
    }
}
