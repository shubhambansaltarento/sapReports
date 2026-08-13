package com.sapreport.dynpro.report.metadata;

/**
 * Which rows an action applies to. {@code CLIENT_ONLY} is declared purely so
 * the UI can render the button from metadata (e.g. a form-reset "Clear")
 * without hardcoding it — it has no server handler and the action endpoint
 * rejects any attempt to invoke it.
 */
public enum ActionScope {
    ROW,
    BULK,
    SELECTION,
    CLIENT_ONLY
}
