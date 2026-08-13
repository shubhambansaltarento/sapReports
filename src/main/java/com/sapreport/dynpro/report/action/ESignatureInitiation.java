package com.sapreport.dynpro.report.action;

/**
 * Result of starting a signing flow. The provider hosts the actual signing
 * capture (confirmed: a third-party e-sign vendor, not TVSM or the dealer)
 * and completion arrives later via a webhook — not built in this
 * deliverable — so this is deliberately a "request accepted" result, never a
 * final status.
 */
public record ESignatureInitiation(String signingUrl, String externalReference) {
}
