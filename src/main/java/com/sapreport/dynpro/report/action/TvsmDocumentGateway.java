package com.sapreport.dynpro.report.action;

/**
 * Port to push a completed, signed document to TVSM. Transport is
 * deliberately not implemented here (see {@link ESignatureGateway}'s
 * javadoc for the same rationale) — a real implementation must source
 * timeouts/retries from configuration, not hardcode them.
 */
public interface TvsmDocumentGateway {

    void push(TvsmPushRequest request);
}
