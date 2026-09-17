package com.sapreport.dynpro.report.partspackinglist;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone endpoint that runs the PARTS_PACKING_LIST query live against
 * Databricks. dealerCode/fromDate/toDate come from the request payload (not
 * query params), per spec (see
 * {@code reports/databricks-scripts/parts-packing-list-query-2026-09-17_180000.md}).
 */
@RestController
@RequestMapping("/parts-packing-list")
@Tag(name = "Parts Packing List (Databricks)", description = "Direct Databricks-backed parts packing list fetch")
public class PartsPackingListDatabricksController {

    private final PartsPackingListDatabricksService partsPackingListDatabricksService;

    public PartsPackingListDatabricksController(PartsPackingListDatabricksService partsPackingListDatabricksService) {
        this.partsPackingListDatabricksService = partsPackingListDatabricksService;
    }

    @GetMapping("/config")
    @Operation(summary = "Get parts packing list's parameter metadata",
            description = "Basic config describing what fetchDatabricksdata expects: dealerCode, fromDate, toDate.")
    public PartsPackingListConfigResponse getConfig() {
        return new PartsPackingListConfigResponse("PARTS_PACKING_LIST", "Parts Packing List", List.of(
                new PartsPackingListConfigResponse.ParameterConfig("dealerCode", "Dealer Code", "STRING", true),
                new PartsPackingListConfigResponse.ParameterConfig("fromDate", "From Date", "DATE", true),
                new PartsPackingListConfigResponse.ParameterConfig("toDate", "To Date", "DATE", true)
        ));
    }

    @PostMapping("/fetchDatabricksdata")
    @Operation(summary = "Run the parts packing list query live against Databricks",
            description = "Runs fn_parts_packing_list for the dealerCode/fromDate/toDate given in the request body.")
    public List<Map<String, Object>> fetchDatabricksdata(@RequestBody PartsPackingListRequest request) {
        return partsPackingListDatabricksService.fetch(request.dealerCode(), request.fromDate(), request.toDate());
    }
}
