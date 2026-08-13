package com.sapreport.dynpro.report.metadata;

/** Confirmation prompt the UI must show before invoking an action; null means no prompt. */
public record ConfirmSpec(String title, String message) {
}
