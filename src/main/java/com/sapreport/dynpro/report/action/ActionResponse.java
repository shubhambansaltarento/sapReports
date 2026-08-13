package com.sapreport.dynpro.report.action;

import java.util.List;

/**
 * Response body of {@code POST /api/v1/reports/{reportCode}/actions/{actionKey}}.
 * Always {@code 200} — partial row failure is represented in {@code results},
 * never surfaced as an HTTP error status.
 */
public record ActionResponse(String actionKey, ActionSummary summary, List<RowActionResult> results) {
    public ActionResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
