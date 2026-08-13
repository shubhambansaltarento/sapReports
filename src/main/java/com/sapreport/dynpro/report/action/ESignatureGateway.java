package com.sapreport.dynpro.report.action;

/**
 * Port to the external e-signature provider. Transport (HTTP client,
 * timeouts, retries) is deliberately not implemented here — a real
 * implementation must source those from configuration, not hardcode them —
 * this interface only fixes the shape so {@code initiateESign} can be built
 * and tested against a stub while the real integration is wired in
 * separately, same pattern as {@code ReportQueryExecutor}.
 */
public interface ESignatureGateway {

    ESignatureInitiation initiate(ESignatureRequest request);
}
