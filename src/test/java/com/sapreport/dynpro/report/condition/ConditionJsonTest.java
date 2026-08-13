package com.sapreport.dynpro.report.condition;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesEq() throws Exception {
        Condition condition = mapper.readValue(
                "{\"field\":\"withCblDetails\",\"op\":\"eq\",\"value\":true}", Condition.class);
        assertThat(condition).isEqualTo(new Condition.Eq("withCblDetails", true));
    }

    @Test
    void deserializesAndOfNested() throws Exception {
        Condition condition = mapper.readValue("""
                {"op":"and","conditions":[
                    {"field":"a","op":"eq","value":"x"},
                    {"field":"b","op":"notEmpty"}
                ]}
                """, Condition.class);
        assertThat(condition).isEqualTo(new Condition.And(java.util.List.of(
                new Condition.Eq("a", "x"),
                new Condition.NotEmpty("b"))));
    }

    @Test
    void roundTripsThroughSerializeAndDeserialize() throws Exception {
        Condition original = new Condition.Or(java.util.List.of(
                new Condition.In("region", java.util.List.of("NORTH", "SOUTH")),
                new Condition.NotEmpty("remarks")));

        String json = mapper.writeValueAsString(original);
        Condition roundTripped = mapper.readValue(json, Condition.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void nullIsPreserved() throws Exception {
        Condition condition = mapper.readValue("null", Condition.class);
        assertThat(condition).isNull();
    }
}
