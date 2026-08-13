package com.sapreport.dynpro.report.metadata;

import java.util.List;

/**
 * A cross-parameter constraint over a set of {@code CHECKBOX}-style
 * {@code members} (e.g. "select only one of divisionVehicle/divisionSpares").
 * Evaluated by {@code ReportParameterValidator} against the coerced
 * parameter values; a violation attaches a {@link com.sapreport.dynpro.report.validation.ValidationError}
 * to {@code key} rather than to any individual member. {@code resolvesTo} is
 * optional — when present and the constraint isn't violated, the validator
 * also derives a single internal value from whichever member is selected.
 */
public record ParameterGroup(
        String key,
        String label,
        String hint,
        List<String> members,
        ParameterGroupConstraint constraint,
        String errorCode,
        GroupResolution resolvesTo
) {
    public ParameterGroup {
        members = members == null ? List.of() : List.copyOf(members);
    }

    /** Pre-resolution constructor, kept so existing metadata/tests need no changes. */
    public ParameterGroup(String key, String label, String hint, List<String> members,
                           ParameterGroupConstraint constraint, String errorCode) {
        this(key, label, hint, members, constraint, errorCode, null);
    }
}
