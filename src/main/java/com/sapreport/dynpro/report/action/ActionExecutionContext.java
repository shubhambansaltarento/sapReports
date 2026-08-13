package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.metadata.ActionDefinition;
import com.sapreport.dynpro.report.security.ReportCallerContext;

import java.util.Map;

/**
 * Everything a {@link ReportActionHandler} needs to apply one row's change.
 * {@code idempotencyKey} is the same key the framework used for its own
 * replay short-circuit — side-effecting actions calling out to an external
 * gateway (e.g. {@link ESignatureGateway}) must carry it through so the
 * gateway can dedupe too, not just this service.
 */
public record ActionExecutionContext(
        String reportCode,
        ActionDefinition action,
        Map<String, Object> parameters,
        Map<String, Object> currentRow,
        ReportCallerContext caller,
        String idempotencyKey
) {
}
