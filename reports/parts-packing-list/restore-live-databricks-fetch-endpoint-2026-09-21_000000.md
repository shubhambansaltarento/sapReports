# Spec: Restore standalone live-Databricks fetch endpoint for Parts Packing List

## Context
Before the generic-report-framework migration
(`migrate-to-generic-report-framework-2026-09-18_000000.md`), Parts Packing List had a standalone
`PartsPackingListDatabricksController`/`Service` at `POST /parts-packing-list/fetchDatabricksdata`
(later kebab-cased to `POST /parts-packing-list/fetch-data-bricks-data`,
`rename-fetch-endpoints-kebab-case-2026-09-18_000000.md`). That migration deleted the standalone
controller with no back-compat alias — the live Databricks JDBC call
(`sap_dynpro.bumblebee.fn_parts_packing_list(?, ?, ?)`) only survives inline inside
`StubReportQueryExecutor.executePartsPackingList`, reached via `/api/v1/reports/PARTS_PACKING_LIST/data`.

`WARRANTY_COST` and `DEALER_LEDGER` both still have their own standalone live-Databricks
controllers (`WarrantyCostDatabricksController` at `/warranty-cost/fetch-data-bricks-data`,
`DealerLedgerDatabricksController` at `/dealer-ledger/fetch-data-bricks-data`) alongside their
generic-framework stub `/data` endpoints. Parts Packing List is the odd one out with no standalone
endpoint — `http://localhost:8080/parts-packing-list/fetch-data-bricks-data` currently 404s.

## Goal
Re-add a standalone controller for Parts Packing List mirroring the warranty-cost/dealer-ledger
pattern: `POST /parts-packing-list/fetch-data-bricks-data`, live against Databricks, separate from
the generic engine's `/api/v1/reports/PARTS_PACKING_LIST/data`.

## Changes
New package `com.sapreport.dynpro.report.partspackinglist`:
- `PartsPackingListDatabricksService` — JDBC call to
  `sap_dynpro.bumblebee.fn_parts_packing_list(?, ?, ?)`, same connection-string pattern as
  `WarrantyCostDatabricksService`/`DealerLedgerDatabricksService`.
- `PartsPackingListDatabricksController` — `@RequestMapping("/parts-packing-list")`,
  `POST /parts-packing-list/fetch-data-bricks-data` taking `{dealerCode, fromDate, toDate, limit}`
  as a JSON body (same shape as `WarrantyCostRequest`).
- `PartsPackingListRequest` — request record, mirrors `WarrantyCostRequest`.
- `PartsPackingListDatabricksQueryException` — mirrors the other two reports' exception classes.
  (`com.sapreport.dynpro.report.query.PartsPackingListQueryException` already exists for the
  generic-framework path; this is a separate exception type for the standalone controller's
  package, matching how `WarrantyCostDatabricksQueryException` sits next to its controller.)

`StubReportQueryExecutor.executePartsPackingList` is left untouched — no change to
`/api/v1/reports/PARTS_PACKING_LIST/data`.

## Out of scope
- No change to `/api/v1/reports/PARTS_PACKING_LIST/config` or `/data`.
- No change to `PartsPackingListQueryRunner` (CLI).
- No back-compat alias for the old `/fetchDatabricksdata` (pre-kebab-case) path — only the
  current kebab-case path is restored.

## Verification
- `mvn -q compile` succeeds.
- `curl -X POST /parts-packing-list/fetch-data-bricks-data` with a sample
  dealerCode/fromDate/toDate returns 200 (live Databricks call; empty array is an acceptable
  result if no matching rows).
