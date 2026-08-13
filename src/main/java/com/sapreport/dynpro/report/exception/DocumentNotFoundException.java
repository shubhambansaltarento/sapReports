package com.sapreport.dynpro.report.exception;

/** Thrown when {@code documentKey} doesn't resolve to a document — also used for an out-of-scope key, so existence isn't leaked. */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String documentKey) {
        super("No such document: " + documentKey);
    }
}
