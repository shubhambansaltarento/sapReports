package com.sapreport.dynpro.report.warrantycost;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone endpoint that runs the WARRANTY_COST query live against
 * Databricks. dealerCode/fromDate/toDate come from the request payload, same
 * shape as {@code PartsPackingListDatabricksController}. Deliberately
 * separate from {@code /api/v1/reports/WARRANTY_COST/data}, which still
 * serves stub data via {@code StubReportQueryExecutor}.
 */
@RestController
@RequestMapping("/warranty-cost")
@Tag(name = "Warranty Cost (Databricks)", description = "Direct Databricks-backed warranty cost fetch")
public class WarrantyCostDatabricksController {

    private final WarrantyCostDatabricksService warrantyCostDatabricksService;

    public WarrantyCostDatabricksController(WarrantyCostDatabricksService warrantyCostDatabricksService) {
        this.warrantyCostDatabricksService = warrantyCostDatabricksService;
    }

    @PostMapping("/fetch-data-bricks-data")
    @Operation(summary = "Run the warranty cost query live against Databricks",
            description = "Runs api_warranty_cost for the dealerCode/fromDate/toDate given in the request body.")
    public List<Map<String, Object>> fetchDataBricksData(@RequestBody WarrantyCostRequest request) {
        return warrantyCostDatabricksService.fetch(request.dealerCode(), request.fromDate(), request.toDate(), request.effectiveLimit());
    }
}
