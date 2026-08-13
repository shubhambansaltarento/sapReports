package com.sapreport.dynpro.report.document;

import com.sapreport.dynpro.report.action.ReportRowStore;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Renders {@code WARRANTY_LABOUR_ORDER_DETAILS}'s invoice PDF on demand from
 * the row's own current data (confirmed) — {@code documentKey} is simply the
 * row's own {@code _rowKey}, so no separate key-issuing step is needed
 * beyond what {@code /data} already computed.
 *
 * <p><b>Stub note</b>: this emits a plain-text placeholder, not a real PDF —
 * generating an actual PDF needs a rendering library (e.g. PDFBox/iText),
 * which isn't added here without asking first. A real implementation would
 * swap the placeholder body for real rendering and change the content type
 * to {@code application/pdf}; the {@link ReportDocumentStore} contract
 * itself doesn't change.
 */
@Component
public class WarrantyLabourInvoicePdfDocumentStore implements ReportDocumentStore {

    static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";

    private final ReportRowStore rowStore;

    public WarrantyLabourInvoicePdfDocumentStore(ReportRowStore rowStore) {
        this.rowStore = rowStore;
    }

    @Override
    public boolean supports(String reportCode) {
        return REPORT_CODE.equals(reportCode);
    }

    @Override
    public Optional<DocumentContent> fetch(String reportCode, String documentKey) {
        return rowStore.find(reportCode, documentKey).map(row -> renderPlaceholder(documentKey, row));
    }

    private DocumentContent renderPlaceholder(String rowKey, Map<String, Object> row) {
        String body = "Invoice PDF placeholder (stub — no PDF renderer wired in yet)\n"
                + "Order number: " + row.get("orderNumber") + "\n"
                + "Reference credit memo no.: " + row.get("referenceCreditMemoNo") + "\n"
                + "Dealer invoice: " + row.get("dealerInvoice") + "\n"
                + "Invoice date: " + row.get("invoiceDate") + "\n"
                + "Labour amount: " + row.get("labourAmount") + " " + row.get("currency") + "\n";
        String fileName = "invoice-" + rowKey.replace("|", "-") + ".txt";
        return new DocumentContent(body.getBytes(StandardCharsets.UTF_8), "text/plain", fileName);
    }
}
