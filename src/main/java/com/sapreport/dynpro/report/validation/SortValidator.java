package com.sapreport.dynpro.report.validation;

import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.query.SortSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Confirms every requested sort field is both currently visible
 * ({@code effectiveColumns}) and marked {@code sortable} in the report's
 * metadata, so a sort field can never be interpolated into SQL beyond a
 * value we've already validated as a known, sortable column.
 */
@Component
public class SortValidator {

    private static final Set<String> VALID_DIRECTIONS = Set.of("asc", "desc");

    public void validate(List<SortSpec> sort, List<String> effectiveColumns,
                          Map<String, ColumnDefinition> columnsByField) {
        if (sort == null || sort.isEmpty()) {
            return;
        }
        Set<String> visible = Set.copyOf(effectiveColumns);
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < sort.size(); i++) {
            SortSpec spec = sort.get(i);
            String fieldPath = "sort[" + i + "].field";
            if (spec.field() == null || !visible.contains(spec.field())) {
                errors.add(new ValidationError(fieldPath, ValidationErrorCodes.INVALID_SORT_FIELD,
                        "'" + spec.field() + "' is not a currently visible column"));
                continue;
            }
            ColumnDefinition column = columnsByField.get(spec.field());
            if (column == null || !column.sortable()) {
                errors.add(new ValidationError(fieldPath, ValidationErrorCodes.INVALID_SORT_FIELD,
                        "'" + spec.field() + "' is not sortable"));
                continue;
            }
            String dir = spec.dir() == null ? null : spec.dir().toLowerCase();
            if (!VALID_DIRECTIONS.contains(dir)) {
                errors.add(new ValidationError("sort[" + i + "].dir", ValidationErrorCodes.INVALID_SORT_FIELD,
                        "Sort direction must be 'asc' or 'desc'"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
