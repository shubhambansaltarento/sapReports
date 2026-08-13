package com.sapreport.dynpro.report.api;

/** Dealer identity for display, derived from the caller — never accepted as a request parameter. */
public record ReportContext(String dealerCode, String dealerDescription) {
}
