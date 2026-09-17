package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.exception.ConfigVersionMismatchException;
import com.sapreport.dynpro.report.exception.ReportAccessDeniedException;
import com.sapreport.dynpro.report.exception.ReportNotFoundException;
import com.sapreport.dynpro.report.metadata.ClasspathReportMetadataRepository;
import com.sapreport.dynpro.report.metadata.ControlType;
import com.sapreport.dynpro.report.metadata.OptionItem;
import com.sapreport.dynpro.report.metadata.PagingConfig;
import com.sapreport.dynpro.report.metadata.ParameterDataType;
import com.sapreport.dynpro.report.metadata.ParameterDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import com.sapreport.dynpro.report.action.InMemoryReportRowStore;
import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.query.SortSpec;
import com.sapreport.dynpro.report.query.StubReportQueryExecutor;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import com.sapreport.dynpro.report.validation.SortValidator;
import com.sapreport.dynpro.report.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ReportServiceTest {

    private ReportService reportService;

    private static final String CONFIG_VERSION = "2026.09.3";

    @BeforeEach
    void setUp() throws IOException {
        ClasspathReportMetadataRepository metadataRepository = new ClasspathReportMetadataRepository();
        metadataRepository.loadMetadata();

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

        reportService = new ReportService(metadataRepository, new ReportParameterValidator(new ReportLookupProviderRegistry(List.of())), new SortValidator(),
                new StubReportQueryExecutor(), callerContext, new InMemoryReportRowStore());
    }

    private Map<String, Object> validParameters(boolean withCblDetails) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("companyCode", "TVSL");
        parameters.put("postingDate", Map.of("from", "2026-04-01", "to", "2026-08-13"));
        parameters.put("withCblDetails", withCblDetails);
        return parameters;
    }

    @Test
    void getConfig_includesDealerContextDerivedFromCaller() {
        ReportConfigResponse config = reportService.getConfig("DEALER_LEDGER");

        assertThat(config.reportCode()).isEqualTo("DEALER_LEDGER");
        assertThat(config.context().dealerCode()).isEqualTo("1130");
        assertThat(config.context().dealerDescription()).isEqualTo("PAWAN SARKAR AUTOMOBILES");
    }

    @Test
    void happyPath_returnsRowsAndPaging() {
        ReportDataRequest request = new ReportDataRequest(validParameters(true),
                new PagingRequest(1, 50), List.of(new SortSpec("docDate", "asc")), CONFIG_VERSION);

        ReportDataResponse response = reportService.getData("DEALER_LEDGER", request);

        assertThat(response.reportCode()).isEqualTo("DEALER_LEDGER");
        assertThat(response.rows()).isNotEmpty();
        assertThat(response.paging().page()).isEqualTo(1);
        assertThat(response.paging().totalRows()).isGreaterThan(0);
        // Pagination is UI-controlled for DEALER_LEDGER (dealer-ledger-pagination spec):
        // the executor returns every row in one page, so pageSize mirrors totalRows and
        // totalPages is always 1.
        assertThat(response.paging().pageSize()).isEqualTo(response.paging().totalRows());
        assertThat(response.paging().totalPages()).isEqualTo(1);
        assertThat(response.rows()).hasSize((int) response.paging().totalRows());
    }

    @Test
    void columnGroupToggling_hidesCblColumnWhenFlagFalse() {
        ReportDataRequest request = new ReportDataRequest(validParameters(false),
                null, List.of(), CONFIG_VERSION);

        ReportDataResponse response = reportService.getData("DEALER_LEDGER", request);

        assertThat(response.effectiveColumns()).extracting(EffectiveColumn::columnName)
                .contains("docDate", "debit", "credit");
        assertThat(response.effectiveColumns()).extracting(EffectiveColumn::columnName)
                .doesNotContain("cblRefNo");
        assertThat(response.rows()).allSatisfy(row -> assertThat(row).doesNotContainKey("cblRefNo"));
    }

    @Test
    void columnGroupToggling_showsCblColumnWhenFlagTrue() {
        ReportDataRequest request = new ReportDataRequest(validParameters(true),
                null, List.of(), CONFIG_VERSION);

        ReportDataResponse response = reportService.getData("DEALER_LEDGER", request);

        assertThat(response.effectiveColumns()).extracting(EffectiveColumn::columnName)
                .contains("cblRefNo");
    }

    @Test
    void configVersionMismatch_throws() {
        ReportDataRequest request = new ReportDataRequest(validParameters(true), null, List.of(), "stale-version");

        ConfigVersionMismatchException ex = catchThrowableOfType(
                () -> reportService.getData("DEALER_LEDGER", request), ConfigVersionMismatchException.class);

        assertThat(ex.expected()).isEqualTo(CONFIG_VERSION);
        assertThat(ex.submitted()).isEqualTo("stale-version");
    }

    @Test
    void unknownParameter_throwsValidationException() {
        Map<String, Object> parameters = validParameters(true);
        parameters.put("notARealParameter", "x");
        ReportDataRequest request = new ReportDataRequest(parameters, null, List.of(), CONFIG_VERSION);

        ValidationException ex = catchThrowableOfType(
                () -> reportService.getData("DEALER_LEDGER", request), ValidationException.class);

        assertThat(ex.errors()).anySatisfy(error -> assertThat(error.field()).isEqualTo("notARealParameter"));
    }

    @Test
    void sortOnColumnHiddenByToggle_isRejected() {
        ReportDataRequest request = new ReportDataRequest(validParameters(false), null,
                List.of(new SortSpec("cblRefNo", "asc")), CONFIG_VERSION);

        ValidationException ex = catchThrowableOfType(
                () -> reportService.getData("DEALER_LEDGER", request), ValidationException.class);

        assertThat(ex.errors()).isNotEmpty();
    }

    @Test
    void companyCodeOutsideAuthorizedScope_isForbidden() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, true, null,
                List.of(new OptionItem("TVSL", "TVS Lucas"), new OptionItem("OTHERCO", "Other Co")), null, null);
        ReportMetadata metadata = new ReportMetadata("SCOPED_REPORT", "Scoped Report", CONFIG_VERSION,
                List.of(companyCode), List.of(), null, new PagingConfig(50, 1000));
        ReportMetadataRepository scopedRepository = code ->
                "SCOPED_REPORT".equals(code) ? Optional.of(metadata) : Optional.empty();

        ReportCallerContext restrictedCaller = new ReportCallerContext() {
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

        ReportService scopedService = new ReportService(scopedRepository, new ReportParameterValidator(new ReportLookupProviderRegistry(List.of())),
                new SortValidator(), new StubReportQueryExecutor(), restrictedCaller, new InMemoryReportRowStore());

        ReportDataRequest request = new ReportDataRequest(Map.of("companyCode", "OTHERCO"), null, List.of(),
                CONFIG_VERSION);

        assertThat(catchThrowableOfType(() -> scopedService.getData("SCOPED_REPORT", request),
                ReportAccessDeniedException.class)).isNotNull();
    }

    @Test
    void unknownReportCode_throwsNotFound() {
        ReportDataRequest request = new ReportDataRequest(validParameters(true), null, List.of(), CONFIG_VERSION);

        assertThat(catchThrowableOfType(() -> reportService.getData("NOT_A_REPORT", request),
                ReportNotFoundException.class)).isNotNull();
    }
}
