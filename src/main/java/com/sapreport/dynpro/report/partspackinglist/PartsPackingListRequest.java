package com.sapreport.dynpro.report.partspackinglist;

import java.time.LocalDate;

/** Request payload for {@code POST /parts-packing-list/fetchDatabricksdata}. */
public record PartsPackingListRequest(String dealerCode, LocalDate fromDate, LocalDate toDate) {
}
