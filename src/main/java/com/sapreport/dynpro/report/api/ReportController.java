package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.action.ActionRequest;
import com.sapreport.dynpro.report.action.ActionResponse;
import com.sapreport.dynpro.report.action.ReportActionService;
import com.sapreport.dynpro.report.document.DocumentContent;
import com.sapreport.dynpro.report.document.ReportDocumentService;
import com.sapreport.dynpro.report.lookup.LookupResponse;
import com.sapreport.dynpro.report.lookup.ReportLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * One generic controller serving every report — {@code reportCode} selects
 * which report's metadata/query runs; there is no per-report controller.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Metadata-driven report configuration and data APIs")
public class ReportController {

    private final ReportService reportService;
    private final ReportActionService reportActionService;
    private final ReportLookupService reportLookupService;
    private final ReportDocumentService reportDocumentService;

    public ReportController(ReportService reportService, ReportActionService reportActionService,
                             ReportLookupService reportLookupService, ReportDocumentService reportDocumentService) {
        this.reportService = reportService;
        this.reportActionService = reportActionService;
        this.reportLookupService = reportLookupService;
        this.reportDocumentService = reportDocumentService;
    }

    @GetMapping("/{reportCode}/config")
    @Operation(summary = "Get a report's parameter/column metadata",
            description = "Returns everything needed to drive the report's search form and result grid: "
                    + "parameter definitions, column groups (with visibleWhen conditions), export formats, and paging limits.")
    public ReportConfigResponse getConfig(
            @Parameter(description = "Report key, e.g. DEALER_LEDGER") @PathVariable String reportCode) {
        return reportService.getConfig(reportCode);
    }

    @PostMapping("/{reportCode}/data")
    @Operation(summary = "Run a report and get its data",
            description = "Validates the submitted parameters against the report's metadata, computes which "
                    + "columns are currently visible, and returns the report's rows/totals/paging.")
    public ReportDataResponse getData(
            @Parameter(description = "Report key, e.g. DEALER_LEDGER") @PathVariable String reportCode,
            @RequestBody ReportDataRequest request) {
        return reportService.getData(reportCode, request);
    }

    @PostMapping("/{reportCode}/actions/{actionKey}")
    @Operation(summary = "Run a row/bulk action for a report",
            description = "Applies edits and/or a side-effecting action to one or more rows, identified by "
                    + "rowKey/rowVersion from a prior data response. Always 200 — partial row failure is "
                    + "represented per-row in the response, not as an HTTP error.")
    public ActionResponse executeAction(
            @Parameter(description = "Report key, e.g. WARRANTY_LABOUR_CHARGE") @PathVariable String reportCode,
            @Parameter(description = "Action key declared in the report's config, e.g. saveOrderDetails") @PathVariable String actionKey,
            @RequestBody ActionRequest request) {
        return reportActionService.execute(reportCode, actionKey, request);
    }

    @GetMapping("/{reportCode}/lookups/{parameterName}")
    @Operation(summary = "Search F4 value-help options for a LOOKUP parameter",
            description = "The same source a submitted parameter value is re-validated against server-side on the data call.")
    public LookupResponse lookup(
            @Parameter(description = "Report key, e.g. WARRANTY_LABOUR_CHARGE") @PathVariable String reportCode,
            @Parameter(description = "Parameter name, e.g. salesOrganisation") @PathVariable String parameterName,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return reportLookupService.search(reportCode, parameterName, query, page, pageSize);
    }

    @GetMapping("/{reportCode}/documents/{documentKey}")
    @Operation(summary = "Download a report document",
            description = "documentKey is opaque and server-issued elsewhere; never a client-constructed path.")
    public ResponseEntity<byte[]> getDocument(
            @Parameter(description = "Report key, e.g. WARRANTY_LABOUR_CHARGE") @PathVariable String reportCode,
            @Parameter(description = "Opaque, server-issued document key") @PathVariable String documentKey) {
        DocumentContent document = reportDocumentService.fetch(reportCode, documentKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }
}
