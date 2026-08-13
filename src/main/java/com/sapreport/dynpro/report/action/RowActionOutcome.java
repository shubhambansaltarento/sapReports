package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a {@link ReportActionHandler} decides for one row, once the generic
 * engine has already confirmed the row exists and its {@code rowVersion}
 * matches. {@code FORBIDDEN_ROW} (out-of-scope rows) and any business
 * failure (e.g. a duplicate-invoice check) are both just a {@link Failure}
 * with the appropriate {@link ValidationError} code — the handler decides
 * the business meaning, the engine only decides row status.
 */
public sealed interface RowActionOutcome {

    record Success(Map<String, Object> updatedFields) implements RowActionOutcome {
        public Success {
            // A handler may legitimately set a field to null (clearing it) — Map.copyOf would reject that.
            updatedFields = updatedFields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(updatedFields));
        }
    }

    record Failure(List<ValidationError> errors) implements RowActionOutcome {
        public Failure {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }
}
