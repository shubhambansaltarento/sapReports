package com.sapreport.dynpro.report.metadata;

/**
 * Scalar data type of a report parameter's value, used to coerce and
 * validate incoming request values. Null on a {@link ParameterDefinition}
 * for controls (e.g. {@link ControlType#DATE_RANGE}) whose value isn't a
 * single scalar.
 */
public enum ParameterDataType {
    STRING,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    DATE
}
