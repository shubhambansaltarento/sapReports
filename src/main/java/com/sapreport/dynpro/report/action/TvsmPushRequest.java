package com.sapreport.dynpro.report.action;

/** What {@link TvsmDocumentGateway#push} needs to push one row's signed document to TVSM. */
public record TvsmPushRequest(String reportCode, String rowKey, String idempotencyKey) {
}
