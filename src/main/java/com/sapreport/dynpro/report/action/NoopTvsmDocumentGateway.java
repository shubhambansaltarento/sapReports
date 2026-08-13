package com.sapreport.dynpro.report.action;

import org.springframework.stereotype.Component;

/** Placeholder {@link TvsmDocumentGateway} — no real network call; swap in the real TVSM integration later. */
@Component
public class NoopTvsmDocumentGateway implements TvsmDocumentGateway {

    @Override
    public void push(TvsmPushRequest request) {
        // No-op: nothing to push to yet.
    }
}
