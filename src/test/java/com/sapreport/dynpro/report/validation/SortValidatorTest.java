package com.sapreport.dynpro.report.validation;

import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.query.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThat;

class SortValidatorTest {

    private final SortValidator validator = new SortValidator();

    private final ColumnDefinition postingDate =
            new ColumnDefinition("postingDate", "Posting Date", "date", "dd-MM-yyyy", "left", true, null);
    private final ColumnDefinition cblRefNo =
            new ColumnDefinition("cblRefNo", "CBL Ref No", "string", null, "left", false, null);

    private final Map<String, ColumnDefinition> columnsByField = Map.of(
            "postingDate", postingDate,
            "cblRefNo", cblRefNo);

    @Test
    void sortOnVisibleSortableColumn_passes() {
        assertThatCode(() -> validator.validate(
                List.of(new SortSpec("postingDate", "asc")),
                List.of("postingDate", "cblRefNo"),
                columnsByField))
                .doesNotThrowAnyException();
    }

    @Test
    void sortOnNonSortableColumn_isRejected() {
        ValidationException ex = catchThrowableOfType(() -> validator.validate(
                        List.of(new SortSpec("cblRefNo", "asc")),
                        List.of("postingDate", "cblRefNo"),
                        columnsByField),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_SORT_FIELD);
    }

    @Test
    void sortOnColumnNotCurrentlyVisible_isRejected() {
        ValidationException ex = catchThrowableOfType(() -> validator.validate(
                        List.of(new SortSpec("cblRefNo", "asc")),
                        List.of("postingDate"),
                        columnsByField),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_SORT_FIELD);
    }

    @Test
    void sortOnUnknownField_isRejectedNeverInterpolated() {
        ValidationException ex = catchThrowableOfType(() -> validator.validate(
                        List.of(new SortSpec("'; DROP TABLE reports; --", "asc")),
                        List.of("postingDate"),
                        columnsByField),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_SORT_FIELD);
    }

    @Test
    void invalidDirection_isRejected() {
        ValidationException ex = catchThrowableOfType(() -> validator.validate(
                        List.of(new SortSpec("postingDate", "sideways")),
                        List.of("postingDate"),
                        columnsByField),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_SORT_FIELD);
    }
}
