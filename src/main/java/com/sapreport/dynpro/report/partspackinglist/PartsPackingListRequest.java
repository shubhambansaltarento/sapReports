package com.sapreport.dynpro.report.partspackinglist;

import java.time.LocalDate;

/** Request payload for {@code POST /parts-packing-list/fetch-data-bricks-data}. */
public record PartsPackingListRequest(String dealerCode, LocalDate fromDate, LocalDate toDate, Integer limit) {

    public int effectiveLimit() {
        return limit != null ? limit : 100;
    }
}
