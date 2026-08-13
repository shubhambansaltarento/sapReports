package com.sapreport.dynpro.report.metadata;

import java.util.List;

/**
 * One search-parameter definition, driving both the Angular form (via
 * {@code control}/{@code options}/{@code layout}) and server-side validation
 * (via {@code dataType}/{@code required}/{@code validation}). {@code lookup}
 * only applies to {@link ControlType#LOOKUP}; {@code multiple} says whether
 * that lookup accepts more than one selected value.
 */
public record ParameterDefinition(
        String name,
        String label,
        ControlType control,
        ParameterDataType dataType,
        boolean required,
        Object defaultValue,
        List<OptionItem> options,
        ValidationRules validation,
        LayoutHint layout,
        Boolean multiple,
        LookupConfig lookup
) {

    /** Pre-LOOKUP-support constructor, kept so existing metadata/tests need no changes. */
    public ParameterDefinition(String name, String label, ControlType control, ParameterDataType dataType,
                                boolean required, Object defaultValue, List<OptionItem> options,
                                ValidationRules validation, LayoutHint layout) {
        this(name, label, control, dataType, required, defaultValue, options, validation, layout, null, null);
    }
}
