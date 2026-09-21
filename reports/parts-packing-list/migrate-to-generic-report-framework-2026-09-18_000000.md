# Spec: Migrate Parts Packing List onto the generic ReportController framework

## Problem
Parts Packing List is a standalone controller/service (`com.sapreport.dynpro.report.partspackinglist`),
outside the generic `/api/v1/reports/{reportCode}/config|data` engine that DEALER_LEDGER and
WARRANTY_COST already use. Its config response has no `context` (dealerCode/dealerDescription)
and no `companyCode` parameter, unlike those reports.

## Goal
Move PARTS_PACKING_LIST onto the generic framework so it gets the same `context`
(dealerCode/dealerDescription, from `ReportCallerContext`) and `companyCode`
authorization-scoped SELECT parameter that DEALER_LEDGER/WARRANTY_COST have, while still
running live against Databricks (not stub data).

## Changes
1. `src/main/resources/reports/parts-packing-list.json` — new metadata file:
   - `reportCode: PARTS_PACKING_LIST`, `configVersion: 2026.09.1`
   - Parameters: `dealerCode` (TEXT, optional), `companyCode` (SELECT, required, default `TSL`,
     options `[{value: TSL, label: "TVS Lucas"}]` — matching dealer-ledger.json/warranty-cost.json),
     `packingDate` (DATE_RANGE, required, both bounds required, minDate 2020-04-01, maxDate $TODAY,
     maxRangeDays 366) replacing the old separate `fromDate`/`toDate` params.
   - `columnGroups`: one `base` group with `dealerCode`, `dealerName` columns (same minimal shape
     as `warranty-cost.json`'s base group) — the Databricks function's exact result schema beyond
     these two fields is unconfirmed (live query returned 0 rows in testing), so no other columns
     are declared yet.
   - `export`: XLSX/PDF, `paging`: default 50 / max 1000 (same as other reports).
2. `StubReportQueryExecutor` — add a `PARTS_PACKING_LIST` branch that runs the real
   `fn_parts_packing_list(dealerCode, fromDate, toDate)` query live against Databricks (JDBC,
   same connection string pattern as `PartsPackingListDatabricksService`), extracting
   fromDate/toDate from the `packingDate` `DateRangeValue` parameter, then projects
   `effectiveColumns` same as other branches. `companyCode` is authorization-only (enforced by
   `ReportService.enforceCompanyCodeScope`), not passed into the query — matching how DEALER_LEDGER
   handles `companyCode` today. Requires adding `@Value`-injected Databricks connection settings
   to this class's constructor.
3. Remove the standalone Parts Packing List package
   (`PartsPackingListDatabricksController`, `PartsPackingListDatabricksService`,
   `PartsPackingListConfigResponse`, `PartsPackingListRequest`,
   `PartsPackingListDatabricksQueryException`) — superseded by the generic endpoints
   `GET /api/v1/reports/PARTS_PACKING_LIST/config` and `POST /api/v1/reports/PARTS_PACKING_LIST/data`.
   `PartsPackingListQueryRunner` (CLI tool) and `parts-packing-list.sql` are untouched.

## Out of scope
- No change to `fn_parts_packing_list`'s actual Databricks-side schema/columns beyond what's
  declared above.
- No UI/Angular changes.

## Verification
- `mvn -q compile` succeeds.
- `GET /api/v1/reports/PARTS_PACKING_LIST/config` returns context + companyCode param.
- `POST /api/v1/reports/PARTS_PACKING_LIST/data` with `companyCode: TSL` and a `packingDate`
  range succeeds (200), talking to live Databricks.
- Old `/parts-packing-list/**` endpoints return 404 (no back-compat alias).
