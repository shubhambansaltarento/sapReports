package com.sapreport.dynpro.report.api;

import java.time.Instant;

/** Diagnostic metadata attached to every data response. */
public record ResponseMeta(Instant generatedAt, Instant dataAsOf, long queryMs) {
}
