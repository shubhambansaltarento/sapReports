package com.sapreport.dynpro.report.metadata;

import java.util.Map;

/**
 * Collapses a checkbox {@link ParameterGroup} into a single internal value
 * before parameters reach the query layer — e.g. {@code divisionVehicle}/
 * {@code divisionSpares} become one {@code division} parameter valued
 * {@code "VEHICLE"}/{@code "SPARES"}. The two booleans stay a UI-fidelity
 * concern only; query/action code only ever sees {@code targetField}.
 */
public record GroupResolution(String targetField, Map<String, String> valueByMember) {
    public GroupResolution {
        valueByMember = valueByMember == null ? Map.of() : Map.copyOf(valueByMember);
    }
}
