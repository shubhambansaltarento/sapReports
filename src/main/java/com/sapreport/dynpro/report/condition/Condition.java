package com.sapreport.dynpro.report.condition;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Structured {@code visibleWhen} condition tree. Deliberately closed and
 * data-only (no SpEL, no scripting, no eval) — the only way to add a new
 * kind of condition is to add a new permitted type here and teach
 * {@link ConditionEvaluator}, {@link ConditionDeserializer}, and
 * {@link ConditionSerializer} about it.
 */
@JsonDeserialize(using = ConditionDeserializer.class)
@JsonSerialize(using = ConditionSerializer.class)
public sealed interface Condition
        permits Condition.Eq, Condition.Neq, Condition.In, Condition.NotEmpty, Condition.And, Condition.Or {

    record Eq(String field, Object value) implements Condition {
    }

    record Neq(String field, Object value) implements Condition {
    }

    record In(String field, List<Object> values) implements Condition {
    }

    record NotEmpty(String field) implements Condition {
    }

    record And(List<Condition> conditions) implements Condition {
    }

    record Or(List<Condition> conditions) implements Condition {
    }
}
