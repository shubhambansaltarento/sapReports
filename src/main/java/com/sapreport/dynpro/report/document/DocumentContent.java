package com.sapreport.dynpro.report.document;

/** A document's bytes plus enough metadata to set Content-Type/Content-Disposition on the response. */
public record DocumentContent(byte[] content, String contentType, String fileName) {
}
