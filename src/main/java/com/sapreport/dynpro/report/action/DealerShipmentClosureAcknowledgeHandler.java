package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code acknowledgeShipments} for {@code DEALER_SHIPMENT_CLOSURE}
 * (a.k.a. Goods Acknowledgement) — a dealer confirming physical receipt of a
 * shipment.
 *
 * <p><b>Confirmed semantics</b> (see the report's spec conversation):
 * <ul>
 *   <li>Only the {@code false -> true} transition is a real acknowledgement.
 *       {@code true -> false} is rejected with {@code ACK_IRREVERSIBLE} —
 *       acknowledgement is permanent. {@code false -> false} and
 *       {@code true -> true} resubmits are harmless idempotent no-ops (the
 *       UI only ever submits rows the dealer actually toggled, and an
 *       already-acknowledged row renders read-only, so these shouldn't occur
 *       in practice — but a same-value resubmit is safe rather than an
 *       error if one ever does).</li>
 *   <li>The downstream write is modeled as synchronous (confirmed: goes
 *       straight to SAP via BAPI/RFC, not a staging table drained later) —
 *       {@link #apply} returns a final, confirmed SUCCESS/FAILED within the
 *       same call. This is a stub simulating that synchronous call; swap in
 *       the real BAPI/RFC integration without changing this contract.</li>
 *   <li>Per-row processing is already independent by construction (the
 *       engine calls {@link #apply} once per row in a loop) — one row's
 *       failure never rolls back another row's already-applied success.</li>
 * </ul>
 *
 * <p><b>Dealer scope</b>: rows carry an internal {@code dealerCode} field
 * even though it isn't one of the report's declared/projected output
 * columns (the query is already dealer-scoped; this is a defense-in-depth
 * check, not the primary scoping mechanism) — a real {@code ReportRowStore}
 * backing implementation is expected to keep that field on the row even
 * though it's never sent to the client.
 */
@Component
public class DealerShipmentClosureAcknowledgeHandler implements ReportActionHandler {

    static final String REPORT_CODE = "DEALER_SHIPMENT_CLOSURE";
    static final String ACTION_KEY = "acknowledgeShipments";

    @Override
    public boolean supports(String reportCode, String actionKey) {
        return REPORT_CODE.equals(reportCode) && ACTION_KEY.equals(actionKey);
    }

    @Override
    public RowActionOutcome apply(ActionExecutionContext context, RowChangeRequest change) {
        Object rowDealerCode = context.currentRow().get("dealerCode");
        if (rowDealerCode != null && !Objects.equals(String.valueOf(rowDealerCode), context.caller().dealerCode())) {
            return new RowActionOutcome.Failure(List.of(new ValidationError("rowKey", ValidationErrorCodes.FORBIDDEN_ROW,
                    "Shipment is outside the caller's dealer scope")));
        }

        Object submitted = change.changes().get("acknowledged");
        if (!(submitted instanceof Boolean nowAcknowledged)) {
            return new RowActionOutcome.Failure(List.of(new ValidationError("acknowledged",
                    ValidationErrorCodes.TYPE_MISMATCH, "acknowledged must be a boolean")));
        }

        boolean wasAcknowledged = Boolean.TRUE.equals(context.currentRow().get("acknowledged"));

        if (wasAcknowledged && !nowAcknowledged) {
            return new RowActionOutcome.Failure(List.of(new ValidationError("acknowledged", "ACK_IRREVERSIBLE",
                    "Acknowledged shipments cannot be reverted")));
        }
        if (!wasAcknowledged && !nowAcknowledged) {
            return new RowActionOutcome.Success(Map.of());
        }

        // The real false->true acknowledgement (or an idempotent true->true resubmit).
        return new RowActionOutcome.Success(Map.of("acknowledged", true));
    }
}
