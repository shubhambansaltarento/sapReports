package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code pushESignToTvsm} for {@code WARRANTY_LABOUR_ORDER_DETAILS}.
 * {@code enabledWhen} ({@code eSignatureStatus == COMPLETED}) is already
 * re-checked generically by {@code ReportActionService} — reaching this
 * handler at all already proves the row is signed. Nothing on the row needs
 * to change on success (no "pushed" column exists) — this is a one-time,
 * confirm-gated fire, safe under idempotent replay via the framework's own
 * idempotency store.
 */
@Component
public class WarrantyLabourPushESignToTvsmHandler implements ReportActionHandler {

    static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";
    static final String ACTION_KEY = "pushESignToTvsm";

    private final TvsmDocumentGateway gateway;

    public WarrantyLabourPushESignToTvsmHandler(TvsmDocumentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean supports(String reportCode, String actionKey) {
        return REPORT_CODE.equals(reportCode) && ACTION_KEY.equals(actionKey);
    }

    @Override
    public RowActionOutcome apply(ActionExecutionContext context, RowChangeRequest change) {
        Object rowDealerCode = context.currentRow().get("dealerCode");
        if (rowDealerCode != null && !Objects.equals(String.valueOf(rowDealerCode), context.caller().dealerCode())) {
            return new RowActionOutcome.Failure(List.of(new ValidationError("rowKey", ValidationErrorCodes.FORBIDDEN_ROW,
                    "Row is outside the caller's dealer scope")));
        }

        gateway.push(new TvsmPushRequest(context.reportCode(), change.rowKey(), context.idempotencyKey()));

        return new RowActionOutcome.Success(Map.of());
    }
}
