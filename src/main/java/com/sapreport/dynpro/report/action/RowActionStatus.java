package com.sapreport.dynpro.report.action;

/** Per-row outcome of an action, distinct from the request-level 422s (unknown action, bad params, configVersion). */
public enum RowActionStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
    STALE
}
