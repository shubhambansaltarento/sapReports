package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code saveOrderDetails} for {@code WARRANTY_LABOUR_ORDER_DETAILS}.
 *
 * <p><b>Confirmed/decided semantics</b>:
 * <ul>
 *   <li>{@code dealerInvoice}/{@code invoiceDate} are a pair — both or
 *       neither ({@code INVOICE_PAIR_INCOMPLETE}), checked against the
 *       row's EFFECTIVE state (submitted change if present, else the
 *       existing value) so an edit that only touches one of the two fields
 *       is judged against what the other field already holds.</li>
 *   <li>{@code invoiceDate} must not be in the future ({@code FUTURE_DATE_NOT_ALLOWED}).</li>
 *   <li>Rows already {@code eSignatureStatus == COMPLETED} are locked
 *       ({@code ROW_LOCKED_AFTER_ESIGN}) — this check is report-specific
 *       business logic, not a generic {@code editValidation} rule engine;
 *       promote to one if a third report needs the same shape (see
 *       {@code DealerShipmentClosureAcknowledgeHandler}'s {@code
 *       ACK_IRREVERSIBLE} for the same precedent).</li>
 *   <li>{@code dealerInvoice} uniqueness is scoped per dealer (confirmed) —
 *       a linear scan of {@link ReportRowStore#findAll} is a placeholder;
 *       a real backing store would enforce this as an indexed/DB
 *       constraint instead.</li>
 * </ul>
 */
@Component
public class WarrantyLabourSaveOrderDetailsHandler implements ReportActionHandler {

    static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";
    static final String ACTION_KEY = "saveOrderDetails";

    private final ReportRowStore rowStore;

    public WarrantyLabourSaveOrderDetailsHandler(ReportRowStore rowStore) {
        this.rowStore = rowStore;
    }

    @Override
    public boolean supports(String reportCode, String actionKey) {
        return REPORT_CODE.equals(reportCode) && ACTION_KEY.equals(actionKey);
    }

    @Override
    public RowActionOutcome apply(ActionExecutionContext context, RowChangeRequest change) {
        Map<String, Object> currentRow = context.currentRow();

        Object rowDealerCode = currentRow.get("dealerCode");
        if (rowDealerCode != null && !Objects.equals(String.valueOf(rowDealerCode), context.caller().dealerCode())) {
            return reject("rowKey", ValidationErrorCodes.FORBIDDEN_ROW, "Row is outside the caller's dealer scope");
        }

        if ("COMPLETED".equals(currentRow.get("eSignatureStatus"))) {
            return reject("acknowledged", "ROW_LOCKED_AFTER_ESIGN", "Row is locked for editing after e-signature completion");
        }

        Object effectiveInvoice = effectiveValue(change, currentRow, "dealerInvoice");
        Object effectiveInvoiceDate = effectiveValue(change, currentRow, "invoiceDate");
        boolean hasInvoice = isPresent(effectiveInvoice);
        boolean hasInvoiceDate = isPresent(effectiveInvoiceDate);

        if (hasInvoice != hasInvoiceDate) {
            return reject("dealerInvoice", "INVOICE_PAIR_INCOMPLETE",
                    "dealerInvoice and invoiceDate must both be set, or both be empty");
        }

        if (hasInvoiceDate) {
            LocalDate invoiceDate;
            try {
                invoiceDate = LocalDate.parse(String.valueOf(effectiveInvoiceDate));
            } catch (RuntimeException e) {
                return reject("invoiceDate", ValidationErrorCodes.TYPE_MISMATCH, "invoiceDate is not a valid date");
            }
            if (invoiceDate.isAfter(LocalDate.now())) {
                return reject("invoiceDate", "FUTURE_DATE_NOT_ALLOWED", "invoiceDate must not be in the future");
            }
        }

        if (hasInvoice && isDuplicateInvoiceForDealer(context, change, rowDealerCode, effectiveInvoice)) {
            return reject("dealerInvoice", "DUPLICATE_INVOICE",
                    "Dealer invoice '" + effectiveInvoice + "' is already used for another order");
        }

        return new RowActionOutcome.Success(change.changes());
    }

    private boolean isDuplicateInvoiceForDealer(ActionExecutionContext context, RowChangeRequest change,
                                                 Object rowDealerCode, Object effectiveInvoice) {
        return rowStore.findAll(context.reportCode()).entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(change.rowKey())
                        && Objects.equals(entry.getValue().get("dealerCode"), rowDealerCode)
                        && Objects.equals(String.valueOf(entry.getValue().get("dealerInvoice")), String.valueOf(effectiveInvoice)));
    }

    private Object effectiveValue(RowChangeRequest change, Map<String, Object> currentRow, String field) {
        return change.changes().containsKey(field) ? change.changes().get(field) : currentRow.get(field);
    }

    private boolean isPresent(Object value) {
        return value != null && !(value instanceof String s && s.isBlank());
    }

    private RowActionOutcome reject(String field, String code, String message) {
        return new RowActionOutcome.Failure(List.of(new ValidationError(field, code, message)));
    }
}
