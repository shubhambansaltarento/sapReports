package com.sapreport.dynpro.report.warrantycost;

import java.time.LocalDate;

/** Request payload for {@code POST /warranty-cost/fetch-data-bricks-data}. */
public record WarrantyCostRequest(String dealerCode, LocalDate fromDate, LocalDate toDate, Integer limit) {

    public int effectiveLimit() {
        return limit != null ? limit : 100;
    }
}
