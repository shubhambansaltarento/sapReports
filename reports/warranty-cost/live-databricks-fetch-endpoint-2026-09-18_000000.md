# Spec: Live Databricks fetch endpoint for Warranty Cost

## Context
WARRANTY_COST already has full `/api/v1/reports/WARRANTY_COST/config` and `/data` (stub data)
via the generic framework. `WarrantyCostQueryRunner` (CLI) already runs the real query
(`api_warranty_cost(dealerCode, fromDate, toDate)`) against Databricks but isn't wired to any
HTTP endpoint. The gap: no standalone live-Databricks fetch endpoint, unlike
`DealerLedgerDatabricksController`/`PartsPackingListDatabricksController`.

## Goal
Add a standalone controller mirroring the dealer-ledger/parts-packing-list pattern, running
live against Databricks — separate from the generic engine's stub `/data`, same as those two.

## Changes
New package `com.sapreport.dynpro.report.warrantycost`:
- `WarrantyCostDatabricksService` — JDBC call to `sap_dynpro.bumblebee.api_warranty_cost(?, ?, ?)`,
  same connection-string pattern as `PartsPackingListDatabricksService`/`DealerLedgerDatabricksService`.
- `WarrantyCostDatabricksController` — `@RequestMapping("/warranty-cost")`,
  `POST /warranty-cost/fetch-data-bricks-data` taking `{dealerCode, fromDate, toDate}` as a JSON
  body (matching Parts Packing List's request shape, since the query takes the same three args).
- `WarrantyCostDatabricksQueryException` — mirrors the other two reports' exception classes.

No `/warranty-cost/config` endpoint is added — `/api/v1/reports/WARRANTY_COST/config` already
covers that.

## Out of scope
- No change to the existing generic-framework WARRANTY_COST config/data (stub) endpoints.
- No change to `WarrantyCostQueryRunner` (CLI).

## Verification
- `mvn -q compile` succeeds.
- `curl -X POST /warranty-cost/fetch-data-bricks-data` with a sample dealerCode/date range
  returns 200 (live Databricks call, empty array is an acceptable result if no matching rows).
