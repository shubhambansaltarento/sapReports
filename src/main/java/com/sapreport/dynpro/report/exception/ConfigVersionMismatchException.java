package com.sapreport.dynpro.report.exception;

/** Thrown when a data request's {@code configVersion} no longer matches the report's current metadata. */
public class ConfigVersionMismatchException extends RuntimeException {

    private final String expected;
    private final String submitted;

    public ConfigVersionMismatchException(String expected, String submitted) {
        super("configVersion mismatch: expected '" + expected + "' but received '" + submitted + "'");
        this.expected = expected;
        this.submitted = submitted;
    }

    public String expected() {
        return expected;
    }

    public String submitted() {
        return submitted;
    }
}
