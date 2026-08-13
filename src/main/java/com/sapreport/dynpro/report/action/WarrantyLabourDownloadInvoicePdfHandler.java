package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code downloadInvoicePdf} for {@code WARRANTY_LABOUR_ORDER_DETAILS}
 * — declared {@code "returns": "DOCUMENT"}, meaning the client is expected to
 * follow a successful response with {@code GET .../documents/{_rowKey}}.
 *
 * <p>The PDF is generated on demand from the row's own data (confirmed) —
 * that rendering lives entirely in {@link WarrantyLabourInvoicePdfDocumentStore},
 * keyed by the row's own {@code _rowKey} (already server-computed, never a
 * client-constructed path). This handler's only job is the dealer-scope
 * check; {@code enabledWhen} (dealerInvoice notEmpty) is already re-checked
 * generically before this runs. Nothing on the row changes, so this is a
 * harmless no-op success either way.
 */
@Component
public class WarrantyLabourDownloadInvoicePdfHandler implements ReportActionHandler {

    static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";
    static final String ACTION_KEY = "downloadInvoicePdf";

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
        return new RowActionOutcome.Success(Map.of());
    }
}
