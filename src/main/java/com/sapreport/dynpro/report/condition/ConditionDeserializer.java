package com.sapreport.dynpro.report.condition;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written deserializer for {@link Condition}, discriminated on the
 * {@code op} field. Kept explicit (no generic polymorphic-type registry)
 * since the shape of each variant differs (field+value vs. field-only vs.
 * nested conditions), not just the discriminator.
 */
public class ConditionDeserializer extends StdDeserializer<Condition> {

    public ConditionDeserializer() {
        super(Condition.class);
    }

    @Override
    public Condition deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        return fromNode(node, ctxt);
    }

    private Condition fromNode(JsonNode node, DeserializationContext ctxt) {
        if (node == null || node.isNull()) {
            return null;
        }
        String op = requiredText(node, "op");
        return switch (op) {
            case "eq" -> new Condition.Eq(requiredText(node, "field"), scalarOf(node.get("value"), ctxt));
            case "neq" -> new Condition.Neq(requiredText(node, "field"), scalarOf(node.get("value"), ctxt));
            case "in" -> new Condition.In(requiredText(node, "field"), scalarsOf(node.get("value"), ctxt));
            case "notEmpty" -> new Condition.NotEmpty(requiredText(node, "field"));
            case "and" -> new Condition.And(conditionsOf(node.get("conditions"), ctxt));
            case "or" -> new Condition.Or(conditionsOf(node.get("conditions"), ctxt));
            default -> throw new IllegalArgumentException("Unsupported visibleWhen op: '" + op + "'");
        };
    }

    private Object scalarOf(JsonNode valueNode, DeserializationContext ctxt) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return ctxt.readTreeAsValue(valueNode, Object.class);
    }

    private List<Object> scalarsOf(JsonNode arrayNode, DeserializationContext ctxt) {
        List<Object> values = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return values;
        }
        for (JsonNode element : arrayNode) {
            values.add(scalarOf(element, ctxt));
        }
        return values;
    }

    private List<Condition> conditionsOf(JsonNode arrayNode, DeserializationContext ctxt) {
        List<Condition> conditions = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return conditions;
        }
        for (JsonNode element : arrayNode) {
            conditions.add(fromNode(element, ctxt));
        }
        return conditions;
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull() || !fieldNode.isTextual()) {
            throw new IllegalArgumentException(
                    "visibleWhen condition missing required text field '" + field + "': " + node);
        }
        return fieldNode.asString();
    }
}
