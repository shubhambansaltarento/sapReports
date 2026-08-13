package com.sapreport.dynpro.report.condition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    @Test
    void nullCondition_isAlwaysVisible() {
        assertThat(ConditionEvaluator.evaluate(null, Map.of())).isTrue();
    }

    @Test
    void eq_matchesEqualValue() {
        Condition condition = new Condition.Eq("withCblDetails", true);
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("withCblDetails", true))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("withCblDetails", false))).isFalse();
    }

    @Test
    void neq_matchesUnequalValue() {
        Condition condition = new Condition.Neq("status", "CLOSED");
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("status", "OPEN"))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("status", "CLOSED"))).isFalse();
    }

    @Test
    void in_matchesMembership() {
        Condition condition = new Condition.In("region", List.of("NORTH", "SOUTH"));
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("region", "SOUTH"))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("region", "EAST"))).isFalse();
    }

    @Test
    void notEmpty_treatsBlankStringAndMissingAsEmpty() {
        Condition condition = new Condition.NotEmpty("remarks");
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("remarks", "hello"))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("remarks", "  "))).isFalse();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of())).isFalse();
    }

    @Test
    void and_requiresAllTrue() {
        Condition condition = new Condition.And(List.of(
                new Condition.Eq("a", true),
                new Condition.Eq("b", true)));
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("a", true, "b", true))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("a", true, "b", false))).isFalse();
    }

    @Test
    void or_requiresAnyTrue() {
        Condition condition = new Condition.Or(List.of(
                new Condition.Eq("a", true),
                new Condition.Eq("b", true)));
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("a", false, "b", true))).isTrue();
        assertThat(ConditionEvaluator.evaluate(condition, Map.of("a", false, "b", false))).isFalse();
    }
}
