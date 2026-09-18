package com.sapreport.dynpro.report.partspackinglist;

import java.util.List;

/** Basic parameter metadata for {@code GET /parts-packing-list/config}. */
public record PartsPackingListConfigResponse(String reportCode, String title, List<ParameterConfig> parameters) {

    public record ParameterConfig(String name, String label, String dataType, boolean required) {
    }
}
