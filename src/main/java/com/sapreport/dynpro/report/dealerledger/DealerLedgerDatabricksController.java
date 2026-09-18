package com.sapreport.dynpro.report.dealerledger;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone endpoint that runs the DEALER_LEDGER query live against
 * Databricks (see {@code reports/dealer-ledge/dealer-ledger-fetch-databricks-endpoint-2026-09-17_160000.md}).
 * Deliberately separate from the generic {@code /api/v1/reports/{reportCode}/data}
 * engine — not backed by {@code ReportQueryExecutor}/{@code StubReportQueryExecutor}.
 */
@RestController
@RequestMapping("/dealer-ledger")
@Tag(name = "Dealer Ledger (Databricks)", description = "Direct Databricks-backed dealer ledger fetch")
public class DealerLedgerDatabricksController {

    private final DealerLedgerDatabricksService dealerLedgerDatabricksService;

    public DealerLedgerDatabricksController(DealerLedgerDatabricksService dealerLedgerDatabricksService) {
        this.dealerLedgerDatabricksService = dealerLedgerDatabricksService;
    }

    @GetMapping("/fetch-data-bricks-data")
    @Operation(summary = "Run the dealer ledger query live against Databricks",
            description = "Runs f_dealer_ledger for the given company code/dealer code over a one-month "
                    + "window starting at fromDate. bukrs defaults to TSL, kunnr to 0000010015, fromDate "
                    + "to one month before today, limit to 100.")
    public List<Map<String, Object>> fetchDatabricksdata(
            @Parameter(description = "Company code (bukrs), default TSL")
            @RequestParam(required = false, defaultValue = "TSL") String bukrs,
            @Parameter(description = "Dealer code (kunnr), default 0000010015")
            @RequestParam(required = false, defaultValue = "0000010015") String kunnr,
            @Parameter(description = "Window start date, default one month before today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Max rows returned, default 100")
            @RequestParam(required = false, defaultValue = "100") int limit) {
        LocalDate effectiveFromDate = fromDate != null ? fromDate : LocalDate.now().minusMonths(1);
        return dealerLedgerDatabricksService.fetchDealerLedgerFromBricks(bukrs, kunnr, effectiveFromDate, limit);
    }
}
