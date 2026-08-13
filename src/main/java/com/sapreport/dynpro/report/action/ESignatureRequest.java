package com.sapreport.dynpro.report.action;

/** What {@link ESignatureGateway#initiate} needs to start a signing flow for one row. */
public record ESignatureRequest(String reportCode, String rowKey, String dealerInvoice, String idempotencyKey) {
}
