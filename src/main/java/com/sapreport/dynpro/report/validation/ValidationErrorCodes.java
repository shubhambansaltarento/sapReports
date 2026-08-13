package com.sapreport.dynpro.report.validation;

/** Stable error codes used in {@link ValidationError#code()}; these drive FE i18n. */
public final class ValidationErrorCodes {

    public static final String REQUIRED = "REQUIRED";
    public static final String UNKNOWN_PARAMETER = "UNKNOWN_PARAMETER";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String INVALID_OPTION = "INVALID_OPTION";
    public static final String MISSING_BOUND = "MISSING_BOUND";
    public static final String MIN_DATE = "MIN_DATE";
    public static final String MAX_DATE = "MAX_DATE";
    public static final String INVALID_RANGE = "INVALID_RANGE";
    public static final String MAX_RANGE_EXCEEDED = "MAX_RANGE_EXCEEDED";
    public static final String INVALID_SORT_FIELD = "INVALID_SORT_FIELD";
    public static final String UNKNOWN_ACTION = "UNKNOWN_ACTION";
    public static final String CONFIG_VERSION_MISMATCH = "CONFIG_VERSION_MISMATCH";
    public static final String INVALID_ROW_COUNT = "INVALID_ROW_COUNT";
    public static final String ROW_NOT_FOUND = "ROW_NOT_FOUND";
    public static final String ROW_VERSION_CONFLICT = "ROW_VERSION_CONFLICT";
    public static final String FORBIDDEN_ROW = "FORBIDDEN_ROW";
    public static final String CLIENT_ONLY_ACTION = "CLIENT_ONLY_ACTION";
    public static final String ACTION_NOT_ENABLED = "ACTION_NOT_ENABLED";

    private ValidationErrorCodes() {
    }
}
