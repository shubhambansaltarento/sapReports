package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.metadata.ClasspathReportMetadataRepository;
import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the four WARRANTY_LABOUR_ORDER_DETAILS action handlers through
 * the real, classpath-loaded metadata — not a fake report/handler — so
 * these also double as a check that the metadata JSON matches what the
 * handlers and the generic engine (rowKey, editableColumns, enabledWhen)
 * expect.
 */
class WarrantyLabourOrderDetailsActionTest {

    private static final String REPORT_CODE = "WARRANTY_LABOUR_ORDER_DETAILS";
    private static final String CALLER_DEALER_CODE = "1130";

    private ReportMetadata metadata;
    private List<ColumnDefinition> editableColumns;
    private InMemoryReportRowStore rowStore;
    private CountingTvsmDocumentGateway tvsmGateway;
    private ReportActionService actionService;

    @BeforeEach
    void setUp() throws IOException {
        ClasspathReportMetadataRepository metadataRepository = new ClasspathReportMetadataRepository();
        metadataRepository.loadMetadata();
        metadata = metadataRepository.findByReportCode(REPORT_CODE).orElseThrow();
        editableColumns = metadata.columnGroups().stream()
                .flatMap(group -> group.columns().stream())
                .filter(ColumnDefinition::editable)
                .toList();

        rowStore = new InMemoryReportRowStore();
        tvsmGateway = new CountingTvsmDocumentGateway();

        ReportCallerContext callerContext = new ReportCallerContext() {
            @Override
            public String dealerCode() {
                return CALLER_DEALER_CODE;
            }

            @Override
            public String dealerDescription() {
                return "PAWAN SARKAR AUTOMOBILES";
            }

            @Override
            public Set<String> authorizedCompanyCodes() {
                return Set.of("TVSL");
            }
        };

        List<ReportActionHandler> handlers = List.of(
                new WarrantyLabourSaveOrderDetailsHandler(rowStore),
                new WarrantyLabourInitiateESignHandler(new NoopESignatureGateway()),
                new WarrantyLabourPushESignToTvsmHandler(tvsmGateway),
                new WarrantyLabourDownloadInvoicePdfHandler());

        actionService = new ReportActionService(metadataRepository,
                new ReportParameterValidator(new ReportLookupProviderRegistry(List.of())),
                new ReportActionHandlerRegistry(handlers), rowStore, new InMemoryIdempotencyStore(), callerContext);
    }

    private Map<String, Object> validParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("orderCreatedDateFrom", "2026-01-01");
        params.put("orderCreatedDateTo", "2026-08-01");
        return params;
    }

    private Map<String, Object> seedRow(String orderNumber, String refCreditMemo, String dealerInvoice,
                                         String invoiceDate, String eSignatureStatus) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNumber", orderNumber);
        row.put("referenceCreditMemoNo", refCreditMemo);
        row.put("labourAmount", "187.2000");
        row.put("currency", "INR");
        row.put("dealerCode", CALLER_DEALER_CODE);
        row.put("dealerInvoice", dealerInvoice);
        row.put("invoiceDate", invoiceDate);
        row.put("eSignatureStatus", eSignatureStatus);
        String rowKey = RowIdentity.computeRowKey(metadata.rowKey(), row);
        rowStore.save(REPORT_CODE, rowKey, row);
        return row;
    }

    private String rowKeyOf(Map<String, Object> row) {
        return RowIdentity.computeRowKey(metadata.rowKey(), row);
    }

    private String versionOf(Map<String, Object> row) {
        return RowIdentity.computeRowVersion(editableColumns, row);
    }

    @Test
    void invoicePairIncomplete_dealerInvoiceWithoutDate_isRejected() {
        Map<String, Object> row = seedRow("ORD-1", "CM-1", null, null, "NOT_STARTED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), null, validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("dealerInvoice", "108"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly("INVOICE_PAIR_INCOMPLETE");
    }

    @Test
    void futureInvoiceDate_isRejected() {
        Map<String, Object> row = seedRow("ORD-2", "CM-2", null, null, "NOT_STARTED");
        String futureDate = LocalDate.now().plusDays(5).toString();

        ActionRequest request = new ActionRequest(metadata.configVersion(), null, validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row),
                        Map.of("dealerInvoice", "109", "invoiceDate", futureDate))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly("FUTURE_DATE_NOT_ALLOWED");
    }

    @Test
    void editOnCompletedRow_isRejected() {
        Map<String, Object> row = seedRow("ORD-3", "CM-3", "110", "2026-01-01", "COMPLETED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), null, validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("dealerInvoice", "111"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly("ROW_LOCKED_AFTER_ESIGN");
    }

    @Test
    void happyPath_saveOrderDetails_succeeds() {
        Map<String, Object> row = seedRow("ORD-4", "CM-4", null, null, "NOT_STARTED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), null, validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row),
                        Map.of("dealerInvoice", "112", "invoiceDate", "2026-08-01"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);
    }

    @Test
    void enabledWhenGating_initiateESign_rejectedWithoutInvoice() {
        Map<String, Object> row = seedRow("ORD-5", "CM-5", null, null, "NOT_STARTED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-esign-gate", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "initiateESign", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly("ACTION_NOT_ENABLED");
    }

    @Test
    void enabledWhenGating_initiateESign_rejectedWhenAlreadyCompleted() {
        Map<String, Object> row = seedRow("ORD-6", "CM-6", "113", "2026-01-01", "COMPLETED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-esign-gate-2", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "initiateESign", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(response.results().get(0).errors()).extracting(e -> e.code()).containsExactly("ACTION_NOT_ENABLED");
    }

    @Test
    void enabledWhenGating_initiateESign_succeedsWhenEligible() {
        Map<String, Object> row = seedRow("ORD-7", "CM-7", "114", "2026-01-01", "NOT_STARTED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-esign-ok", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "initiateESign", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);
    }

    @Test
    void enabledWhenGating_pushESignToTvsm_rejectedWhenNotCompleted() {
        Map<String, Object> row = seedRow("ORD-8", "CM-8", "115", "2026-01-01", "PENDING");

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-push-gate", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "pushESignToTvsm", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(response.results().get(0).errors()).extracting(e -> e.code()).containsExactly("ACTION_NOT_ENABLED");
        assertThat(tvsmGateway.pushCount()).isZero();
    }

    @Test
    void enabledWhenGating_downloadInvoicePdf_rejectedWithoutInvoice() {
        Map<String, Object> row = seedRow("ORD-9", "CM-9", null, null, "NOT_STARTED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), null, validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "downloadInvoicePdf", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(response.results().get(0).errors()).extracting(e -> e.code()).containsExactly("ACTION_NOT_ENABLED");
    }

    @Test
    void idempotentReplay_pushESignToTvsm_doesNotReinvokeGateway() {
        Map<String, Object> row = seedRow("ORD-10", "CM-10", "116", "2026-01-01", "COMPLETED");

        ActionRequest request = new ActionRequest(metadata.configVersion(), "push-idem-key", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of())));

        ActionResponse first = actionService.execute(REPORT_CODE, "pushESignToTvsm", request);
        assertThat(first.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);
        assertThat(tvsmGateway.pushCount()).isEqualTo(1);

        ActionResponse replay = actionService.execute(REPORT_CODE, "pushESignToTvsm", request);

        assertThat(replay).isEqualTo(first);
        assertThat(tvsmGateway.pushCount()).isEqualTo(1);
    }

    private static final class CountingTvsmDocumentGateway implements TvsmDocumentGateway {
        private final AtomicInteger pushes = new AtomicInteger();

        @Override
        public void push(TvsmPushRequest request) {
            pushes.incrementAndGet();
        }

        int pushCount() {
            return pushes.get();
        }
    }
}
