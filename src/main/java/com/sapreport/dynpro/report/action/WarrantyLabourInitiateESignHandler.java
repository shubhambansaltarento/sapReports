package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code initiateESign} for {@code WARRANTY_LABOUR_ORDER_DETAILS}.
 * {@code enabledWhen} (dealerInvoice set, not already COMPLETED) is already
 * re-checked generically by {@code ReportActionService} before this runs —
 * this handler only does the dealer-scope check and the gateway call.
 *
 * <p>Confirmed flow: the e-sign provider is a third-party vendor (not TVSM,
 * not dealer-side); it hosts the actual signing capture and completion
 * arrives later via a webhook (not built here) — so this can only ever move
 * a row to {@code PENDING}, never {@code COMPLETED}, within this call.
 */
@Component
public class WarrantyLabourInitiateESignHandler implements ReportActionHandler {

    static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";
    static final String ACTION_KEY = "initiateESign";

    private final ESignatureGateway gateway;

    public WarrantyLabourInitiateESignHandler(ESignatureGateway gateway) {
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

        Object dealerInvoice = context.currentRow().get("dealerInvoice");
        ESignatureInitiation initiation = gateway.initiate(new ESignatureRequest(
                context.reportCode(), change.rowKey(), String.valueOf(dealerInvoice), context.idempotencyKey()));

        return new RowActionOutcome.Success(Map.of(
                "eSignatureStatus", "PENDING",
                "eSignatureUrl", initiation.signingUrl(),
                "eSignatureReference", initiation.externalReference()));
    }
}
