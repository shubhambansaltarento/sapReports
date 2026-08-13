package com.sapreport.dynpro.report.action;

/** {@code failed} counts every row not SUCCESS (FAILED, STALE, and SKIPPED alike) — {@code results} keeps the precise per-row distinction. */
public record ActionSummary(int requested, int succeeded, int failed) {
}
