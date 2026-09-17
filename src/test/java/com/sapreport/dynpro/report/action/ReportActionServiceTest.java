package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.metadata.ActionDefinition;
import com.sapreport.dynpro.report.metadata.ActionScope;
import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.ColumnGroup;
import com.sapreport.dynpro.report.metadata.ColumnType;
import com.sapreport.dynpro.report.metadata.ConfirmSpec;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import com.sapreport.dynpro.report.metadata.RowKeySpec;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import com.sapreport.dynpro.report.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ReportActionServiceTest {

    private static final String REPORT_CODE = "TEST_REPORT";
    private static final String CONFIG_VERSION = "v1";

    private ColumnDefinition dealerInvoiceColumn;
    private ReportMetadata metadata;
    private InMemoryReportRowStore rowStore;
    private InMemoryIdempotencyStore idempotencyStore;
    private FakeReportActionHandler handler;
    private ReportActionService actionService;

    @BeforeEach
    void setUp() {
        dealerInvoiceColumn = new ColumnDefinition("dealerInvoice", "Dealer Invoice", "string", null, "left",
                false, null, ColumnType.DATA, true, null, null, Map.of(), null, null);
        ColumnDefinition dealerCodeColumn = new ColumnDefinition("dealerCode", "Dealer Code", "string", null, "left", false, null);
        ColumnGroup group = new ColumnGroup("base", "Base", null, List.of(dealerCodeColumn, dealerInvoiceColumn));

        ActionDefinition saveOrderDetails = new ActionDefinition("saveOrderDetails", "Save", ActionScope.BULK,
                "EDITED_ROWS", false, null, null, null, true);
        ActionDefinition pushESign = new ActionDefinition("pushESignToTvsm", "Push to TVSM", ActionScope.ROW,
                null, true, true, new ConfirmSpec("Push to TVSM?", "This cannot be undone."), null, true);

        metadata = new ReportMetadata(REPORT_CODE, "Test Report", CONFIG_VERSION, List.of(), List.of(group), null, null,
                null, null, null, List.of(), new RowKeySpec(List.of("orderNumber"), "|"),
                List.of(saveOrderDetails, pushESign));

        rowStore = new InMemoryReportRowStore();
        idempotencyStore = new InMemoryIdempotencyStore();
        handler = new FakeReportActionHandler();

        ReportMetadataRepository metadataRepository = reportCode ->
                REPORT_CODE.equals(reportCode) ? Optional.of(metadata) : Optional.empty();
        ReportCallerContext callerContext = new ReportCallerContext() {
            @Override
            public String dealerCode() {
                return "1130";
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

        actionService = new ReportActionService(metadataRepository,
                new com.sapreport.dynpro.report.validation.ReportParameterValidator(
                        new com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry(List.of())),
                new ReportActionHandlerRegistry(List.of(handler)), rowStore, idempotencyStore, callerContext);
    }

    private Map<String, Object> seedRow(String rowKey, String dealerCode, String dealerInvoice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dealerCode", dealerCode);
        row.put("dealerInvoice", dealerInvoice);
        rowStore.save(REPORT_CODE, rowKey, row);
        return row;
    }

    private String currentVersion(Map<String, Object> row) {
        return RowIdentity.computeRowVersion(List.of(dealerInvoiceColumn), row);
    }

    @Test
    void partialSuccess_reportsPerRowResults() {
        Map<String, Object> row1 = seedRow("1", "DEALER1", null);
        Map<String, Object> row2 = seedRow("2", "DEALER1", null);

        ActionRequest request = new ActionRequest(CONFIG_VERSION, null, Map.of(), List.of(
                new RowChangeRequest("1", currentVersion(row1), Map.of("dealerInvoice", "108")),
                new RowChangeRequest("2", currentVersion(row2), Map.of("dealerInvoice", "DUPLICATE"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        assertThat(response.summary()).isEqualTo(new ActionSummary(2, 1, 1));
        RowActionResult result1 = response.results().get(0);
        assertThat(result1.rowKey()).isEqualTo("1");
        assertThat(result1.status()).isEqualTo(RowActionStatus.SUCCESS);
        assertThat(result1.errors()).isEmpty();

        RowActionResult result2 = response.results().get(1);
        assertThat(result2.rowKey()).isEqualTo("2");
        assertThat(result2.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result2.errors()).extracting("code").containsExactly("DUPLICATE_INVOICE");
    }

    @Test
    void staleRow_returnsStaleStatusWithCurrentVersion() {
        Map<String, Object> row = seedRow("1", "DEALER1", null);
        String actualVersion = currentVersion(row);

        ActionRequest request = new ActionRequest(CONFIG_VERSION, null, Map.of(), List.of(
                new RowChangeRequest("1", "not-the-real-version", Map.of("dealerInvoice", "108"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.STALE);
        assertThat(result.rowVersion()).isEqualTo(actualVersion);
        assertThat(result.errors()).extracting("code").containsExactly(ValidationErrorCodes.ROW_VERSION_CONFLICT);
        assertThat(handler.invocationCount()).isZero();
    }

    @Test
    void idempotentReplay_returnsOriginalResultWithoutReinvokingHandler() {
        Map<String, Object> row = seedRow("1", "DEALER1", null);
        ActionRequest request = new ActionRequest(CONFIG_VERSION, "replay-key-1", Map.of(), List.of(
                new RowChangeRequest("1", currentVersion(row), Map.of("dealerInvoice", "108"))));

        ActionResponse first = actionService.execute(REPORT_CODE, "pushESignToTvsm", request);
        assertThat(handler.invocationCount()).isEqualTo(1);

        // A second call with the same idempotencyKey, even against a now-stale rowVersion, must short-circuit.
        ActionResponse replay = actionService.execute(REPORT_CODE, "pushESignToTvsm", request);

        assertThat(replay).isEqualTo(first);
        assertThat(handler.invocationCount()).isEqualTo(1);
    }

    @Test
    void sideEffectingAction_withoutIdempotencyKey_isRejected() {
        Map<String, Object> row = seedRow("1", "DEALER1", null);
        ActionRequest request = new ActionRequest(CONFIG_VERSION, null, Map.of(), List.of(
                new RowChangeRequest("1", currentVersion(row), Map.of("dealerInvoice", "108"))));

        ValidationException ex = catchThrowableOfType(
                () -> actionService.execute(REPORT_CODE, "pushESignToTvsm", request), ValidationException.class);

        assertThat(ex.errors()).extracting("code").containsExactly(ValidationErrorCodes.REQUIRED);
        assertThat(handler.invocationCount()).isZero();
    }

    @Test
    void outOfScopeRow_returnsForbiddenRow() {
        Map<String, Object> row = seedRow("1", "OUT_OF_SCOPE", null);

        ActionRequest request = new ActionRequest(CONFIG_VERSION, null, Map.of(), List.of(
                new RowChangeRequest("1", currentVersion(row), Map.of("dealerInvoice", "108"))));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting("code").containsExactly(ValidationErrorCodes.FORBIDDEN_ROW);
    }

    @Test
    void emptyChanges_areSkippedWithoutInvokingHandler() {
        Map<String, Object> row = seedRow("1", "DEALER1", null);

        ActionRequest request = new ActionRequest(CONFIG_VERSION, null, Map.of(), List.of(
                new RowChangeRequest("1", currentVersion(row), Map.of())));

        ActionResponse response = actionService.execute(REPORT_CODE, "saveOrderDetails", request);

        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.SKIPPED);
        assertThat(handler.invocationCount()).isZero();
    }

    /** A fake report+action's business logic: rejects out-of-scope dealers, rejects a magic "DUPLICATE" invoice, else applies the change. */
    private static final class FakeReportActionHandler implements ReportActionHandler {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public boolean supports(String reportCode, String actionKey) {
            return REPORT_CODE.equals(reportCode);
        }

        @Override
        public RowActionOutcome apply(ActionExecutionContext context, RowChangeRequest change) {
            invocations.incrementAndGet();
            if ("OUT_OF_SCOPE".equals(context.currentRow().get("dealerCode"))) {
                return new RowActionOutcome.Failure(List.of(new com.sapreport.dynpro.report.validation.ValidationError(
                        "rowKey", ValidationErrorCodes.FORBIDDEN_ROW, "Row is outside the caller's dealer scope")));
            }
            if ("DUPLICATE".equals(change.changes().get("dealerInvoice"))) {
                return new RowActionOutcome.Failure(List.of(new com.sapreport.dynpro.report.validation.ValidationError(
                        "dealerInvoice", "DUPLICATE_INVOICE", "Dealer invoice already used")));
            }
            return new RowActionOutcome.Success(change.changes());
        }

        int invocationCount() {
            return invocations.get();
        }
    }
}
