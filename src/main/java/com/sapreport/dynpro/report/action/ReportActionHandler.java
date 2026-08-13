package com.sapreport.dynpro.report.action;

/**
 * One report+action's business logic. Resolved by {@code reportCode} +
 * {@code actionKey} through {@link ReportActionHandlerRegistry} — the
 * dispatcher never branches on either itself. Row existence and
 * {@code rowVersion} matching are already confirmed by the time
 * {@link #apply} is called; this is purely business logic (mutate, and/or
 * check the row's out-of-scope/{@code FORBIDDEN_ROW} case, and/or any other
 * per-action business validation).
 */
public interface ReportActionHandler {

    boolean supports(String reportCode, String actionKey);

    RowActionOutcome apply(ActionExecutionContext context, RowChangeRequest change);
}
