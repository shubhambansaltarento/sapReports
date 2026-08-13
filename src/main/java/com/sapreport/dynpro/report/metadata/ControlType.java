package com.sapreport.dynpro.report.metadata;

/**
 * UI control type a report parameter renders as. Drives the Angular form,
 * never business logic beyond validation (e.g. DATE_RANGE parameters carry
 * a {@code from}/{@code to} pair instead of a scalar value).
 */
public enum ControlType {
    TEXT,
    NUMBER,
    SELECT,
    MULTI_SELECT,
    DATE,
    DATE_RANGE,
    CHECKBOX,
    LOOKUP
}
