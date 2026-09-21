package com.sapreport.dynpro.report.partspackinglist;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone endpoint that runs the PARTS_PACKING_LIST query live against
 * Databricks. dealerCode/fromDate/toDate come from the request payload, same
 * shape as {@code WarrantyCostDatabricksController}. Deliberately separate
 * from {@code /api/v1/reports/PARTS_PACKING_LIST/data}, which runs the same
 * query inline via {@code StubReportQueryExecutor}.
 */
@RestController
@RequestMapping("/parts-packing-list")
@Tag(name = "Parts Packing List (Databricks)", description = "Direct Databricks-backed parts packing list fetch")
public class PartsPackingListDatabricksController {

    private final PartsPackingListDatabricksService partsPackingListDatabricksService;

    public PartsPackingListDatabricksController(PartsPackingListDatabricksService partsPackingListDatabricksService) {
        this.partsPackingListDatabricksService = partsPackingListDatabricksService;
    }

    @PostMapping("/fetch-data-bricks-data")
    @Operation(summary = "Run the parts packing list query live against Databricks",
            description = "Runs fn_parts_packing_list for the dealerCode/fromDate/toDate given in the request body.")
    public List<Map<String, Object>> fetchDataBricksData(@RequestBody PartsPackingListRequest request) {
        return partsPackingListDatabricksService.fetch(request.dealerCode(), request.fromDate(), request.toDate(), request.effectiveLimit());
    }
}
