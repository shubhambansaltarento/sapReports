package com.sapreport.dynpro.report.condition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates a {@link Condition} tree against a report's validated/coerced
 * parameter values. This is the single source of truth used both to decide
 * config-response column-group visibility and to compute
 * {@code effectiveColumns} on the data endpoint.
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static boolean evaluate(Condition condition, Map<String, Object> parameters) {
        if (condition == null) {
            return true;
        }
        return switch (condition) {
            case Condition.Eq eq -> valuesEqual(parameters.get(eq.field()), eq.value());
            case Condition.Neq neq -> !valuesEqual(parameters.get(neq.field()), neq.value());
            case Condition.In in -> in.values().stream()
                    .anyMatch(candidate -> valuesEqual(parameters.get(in.field()), candidate));
            case Condition.NotEmpty notEmpty -> isNotEmpty(parameters.get(notEmpty.field()));
            case Condition.And and -> and.conditions().stream().allMatch(c -> evaluate(c, parameters));
            case Condition.Or or -> or.conditions().stream().anyMatch(c -> evaluate(c, parameters));
        };
    }

    private static boolean isNotEmpty(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isBlank();
        }
        if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        return true;
    }

    private static boolean valuesEqual(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        if (actual instanceof Number && expected instanceof Number) {
            return new BigDecimal(actual.toString()).compareTo(new BigDecimal(expected.toString())) == 0;
        }
        return Objects.equals(String.valueOf(actual), String.valueOf(expected)) || Objects.equals(actual, expected);
    }
}
