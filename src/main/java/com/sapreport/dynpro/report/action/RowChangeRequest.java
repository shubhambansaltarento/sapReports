package com.sapreport.dynpro.report.action;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One row's submitted edits within an {@link ActionRequest}. A change value may legitimately be null (clearing a field). */
public record RowChangeRequest(String rowKey, String rowVersion, Map<String, Object> changes) {
    public RowChangeRequest {
        changes = changes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(changes));
    }
}
