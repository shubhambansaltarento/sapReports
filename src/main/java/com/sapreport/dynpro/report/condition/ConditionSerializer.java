package com.sapreport.dynpro.report.condition;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

/** Symmetric counterpart to {@link ConditionDeserializer}: writes each {@link Condition} back as its structured, op-discriminated JSON shape. */
public class ConditionSerializer extends StdSerializer<Condition> {

    public ConditionSerializer() {
        super(Condition.class);
    }

    @Override
    public void serialize(Condition condition, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        if (condition == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        switch (condition) {
            case Condition.Eq eq -> {
                gen.writeStringProperty("field", eq.field());
                gen.writeStringProperty("op", "eq");
                gen.writeName("value");
                writeScalar(gen, eq.value());
            }
            case Condition.Neq neq -> {
                gen.writeStringProperty("field", neq.field());
                gen.writeStringProperty("op", "neq");
                gen.writeName("value");
                writeScalar(gen, neq.value());
            }
            case Condition.In in -> {
                gen.writeStringProperty("field", in.field());
                gen.writeStringProperty("op", "in");
                gen.writeArrayPropertyStart("value");
                for (Object value : in.values()) {
                    writeScalar(gen, value);
                }
                gen.writeEndArray();
            }
            case Condition.NotEmpty notEmpty -> {
                gen.writeStringProperty("field", notEmpty.field());
                gen.writeStringProperty("op", "notEmpty");
            }
            case Condition.And and -> {
                gen.writeStringProperty("op", "and");
                gen.writeArrayPropertyStart("conditions");
                for (Condition nested : and.conditions()) {
                    serialize(nested, gen, provider);
                }
                gen.writeEndArray();
            }
            case Condition.Or or -> {
                gen.writeStringProperty("op", "or");
                gen.writeArrayPropertyStart("conditions");
                for (Condition nested : or.conditions()) {
                    serialize(nested, gen, provider);
                }
                gen.writeEndArray();
            }
        }
        gen.writeEndObject();
    }

    private void writeScalar(JsonGenerator gen, Object value) throws JacksonException {
        switch (value) {
            case null -> gen.writeNull();
            case Boolean b -> gen.writeBoolean(b);
            case Integer i -> gen.writeNumber(i);
            case Long l -> gen.writeNumber(l);
            case Double d -> gen.writeNumber(d);
            case BigDecimal bd -> gen.writeNumber(bd);
            default -> gen.writeString(String.valueOf(value));
        }
    }
}
