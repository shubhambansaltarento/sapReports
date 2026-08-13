package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.condition.Condition;

/** Server-side validation rules for an {@code editable} column's submitted value. */
public record EditValidation(Integer maxLength, String pattern, Condition requiredWhen) {
}
