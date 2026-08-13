package com.sapreport.dynpro.report.action;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request body of {@code POST /api/v1/reports/{reportCode}/actions/{actionKey}}. */
public record ActionRequest(
        String configVersion,
        String idempotencyKey,
        Map<String, Object> parameters,
        List<RowChangeRequest> rows
) {
    public ActionRequest {
        parameters = parameters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
