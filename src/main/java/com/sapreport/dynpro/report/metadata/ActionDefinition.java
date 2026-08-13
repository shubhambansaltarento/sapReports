package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.condition.Condition;

/**
 * One action a report exposes on {@code POST /api/v1/reports/{reportCode}/actions/{actionKey}}.
 * {@code enabledWhen} reuses the same {@link Condition} tree/evaluator as a
 * column's {@code visibleWhen} and {@code action.enabledWhen} — one
 * evaluator, no second implementation, just evaluated against a row instead
 * of the request parameters. It's also re-checked server-side by the
 * generic engine before dispatch — the UI gate is never trusted alone.
 *
 * <p>{@code returns} is {@code null}/omitted for an ordinary row-mutating
 * action; {@code "DOCUMENT"} tells the client that on success, the row's own
 * {@code _rowKey} is a valid {@code documentKey} to fetch from
 * {@code GET .../documents/{documentKey}} — the action response itself
 * stays the same {@code ActionResponse} JSON either way, no separate wire
 * shape for document-producing actions.
 */
public record ActionDefinition(
        String actionKey,
        String label,
        ActionScope scope,
        String appliesTo,
        boolean sideEffecting,
        Boolean idempotent,
        ConfirmSpec confirm,
        Condition enabledWhen,
        boolean refreshAfter,
        String returns
) {
    /** Pre-`returns` constructor, kept so existing metadata/tests need no changes. */
    public ActionDefinition(String actionKey, String label, ActionScope scope, String appliesTo, boolean sideEffecting,
                             Boolean idempotent, ConfirmSpec confirm, Condition enabledWhen, boolean refreshAfter) {
        this(actionKey, label, scope, appliesTo, sideEffecting, idempotent, confirm, enabledWhen, refreshAfter, null);
    }
}
