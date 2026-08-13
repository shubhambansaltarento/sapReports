package com.sapreport.dynpro.report.action;

import org.springframework.stereotype.Component;

/** Placeholder {@link ESignatureGateway} — no real network call; swap in the real vendor integration later. */
@Component
public class NoopESignatureGateway implements ESignatureGateway {

    @Override
    public ESignatureInitiation initiate(ESignatureRequest request) {
        return new ESignatureInitiation(
                "https://esign.example.invalid/sign/" + request.rowKey(),
                "ESIGN-" + request.idempotencyKey());
    }
}
