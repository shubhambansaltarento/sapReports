package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.metadata.ClasspathReportMetadataRepository;
import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@code acknowledgeShipments} end-to-end through the real,
 * classpath-loaded DEALER_SHIPMENT_CLOSURE metadata and the real {@link
 * DealerShipmentClosureAcknowledgeHandler} — not a fake report/handler, so
 * these also double as a check that the metadata JSON is shaped the way the
 * handler and the generic engine expect (rowKey fields, editable column).
 */
class DealerShipmentClosureAcknowledgeHandlerTest {

    private static final String REPORT_CODE = "DEALER_SHIPMENT_CLOSURE";
    private static final String ACTION_KEY = "acknowledgeShipments";
    private static final String CALLER_DEALER_CODE = "1130";

    private ReportMetadata metadata;
    private List<ColumnDefinition> editableColumns;
    private InMemoryReportRowStore rowStore;
    private InMemoryIdempotencyStore idempotencyStore;
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
        idempotencyStore = new InMemoryIdempotencyStore();

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

        actionService = new ReportActionService(metadataRepository,
                new ReportParameterValidator(new ReportLookupProviderRegistry(List.of())),
                new ReportActionHandlerRegistry(List.of(new DealerShipmentClosureAcknowledgeHandler())),
                rowStore, idempotencyStore, callerContext);
    }

    private Map<String, Object> validParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("dateFrom", "2026-08-01");
        params.put("dateTo", "2026-08-10");
        params.put("divisionVehicle", true);
        params.put("divisionSpares", false);
        return params;
    }

    private Map<String, Object> seedShipment(String invoiceNo, String shipmentNo, String dealerCode, boolean acknowledged) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("invoiceNo", invoiceNo);
        row.put("shipmentNo", shipmentNo);
        row.put("date", "2026-08-05");
        row.put("acknowledged", acknowledged);
        row.put("dealerCode", dealerCode);
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
    void acknowledging_falseToTrue_succeeds() {
        Map<String, Object> row = seedShipment("INV-1", "SHIP-1", CALLER_DEALER_CODE, false);

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-happy-path", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("acknowledged", true))));

        ActionResponse response = actionService.execute(REPORT_CODE, ACTION_KEY, request);

        assertThat(response.summary()).isEqualTo(new ActionSummary(1, 1, 0));
        assertThat(response.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);
    }

    @Test
    void unAcknowledging_trueToFalse_isRejected() {
        Map<String, Object> row = seedShipment("INV-2", "SHIP-2", CALLER_DEALER_CODE, true);

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-unack", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("acknowledged", false))));

        ActionResponse response = actionService.execute(REPORT_CODE, ACTION_KEY, request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly("ACK_IRREVERSIBLE");
    }

    @Test
    void staleRow_returnsStaleStatusWithCurrentVersion() {
        Map<String, Object> row = seedShipment("INV-3", "SHIP-3", CALLER_DEALER_CODE, false);
        String actualVersion = versionOf(row);

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-stale", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), "not-the-real-version", Map.of("acknowledged", true))));

        ActionResponse response = actionService.execute(REPORT_CODE, ACTION_KEY, request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.STALE);
        assertThat(result.rowVersion()).isEqualTo(actualVersion);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly(ValidationErrorCodes.ROW_VERSION_CONFLICT);
    }

    @Test
    void idempotentReplay_returnsOriginalResultInsteadOfReprocessing() {
        Map<String, Object> row = seedShipment("INV-4", "SHIP-4", CALLER_DEALER_CODE, false);

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-replay-key", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("acknowledged", true))));

        ActionResponse first = actionService.execute(REPORT_CODE, ACTION_KEY, request);
        assertThat(first.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);

        // The row store now holds the post-acknowledgement state, so the ORIGINAL rowVersion in `request`
        // is stale — if replay were NOT short-circuited, re-processing would report STALE, not SUCCESS.
        ActionResponse replay = actionService.execute(REPORT_CODE, ACTION_KEY, request);

        assertThat(replay).isEqualTo(first);
        assertThat(replay.results().get(0).status()).isEqualTo(RowActionStatus.SUCCESS);
    }

    @Test
    void outOfScopeRow_returnsForbiddenRow() {
        Map<String, Object> row = seedShipment("INV-5", "SHIP-5", "9999-OTHER-DEALER", false);

        ActionRequest request = new ActionRequest(metadata.configVersion(), "idem-oos", validParameters(),
                List.of(new RowChangeRequest(rowKeyOf(row), versionOf(row), Map.of("acknowledged", true))));

        ActionResponse response = actionService.execute(REPORT_CODE, ACTION_KEY, request);

        RowActionResult result = response.results().get(0);
        assertThat(result.status()).isEqualTo(RowActionStatus.FAILED);
        assertThat(result.errors()).extracting(e -> e.code()).containsExactly(ValidationErrorCodes.FORBIDDEN_ROW);
    }
}
