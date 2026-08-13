package com.sapreport.dynpro.report.validation;

import java.time.LocalDate;

/** Coerced value of a {@code DATE_RANGE} control parameter. Either bound may be null. */
public record DateRangeValue(LocalDate from, LocalDate to) {
}
