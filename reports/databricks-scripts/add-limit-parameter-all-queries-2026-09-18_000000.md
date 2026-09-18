# Spec: Accept a `limit` parameter in every Databricks query

## Problem
Only `DEALER_LEDGER`'s standalone service (`DealerLedgerDatabricksService`) bounds its query
with `LIMIT ?`. Every other Databricks-backed query — `PARTS_PACKING_LIST` (both the CLI runner
and the live branch wired into the generic framework's `StubReportQueryExecutor`), `WARRANTY_COST`
(CLI runner and the new standalone live-fetch service), and the CLI-only `DealerLedgerQueryRunner`
— run unbounded. This caused a real `OutOfMemoryError` in testing: an empty `dealerCode` plus a
366-day `packingDate` range on `PARTS_PACKING_LIST` pulled the entire unfiltered table into
memory before any pagination was applied.

## Goal
Every Databricks query accepts a `limit` and applies it server-side via `LIMIT ?`, so no query
can return an unbounded result set.

## Changes
1. `.sql` reference docs (`databricks-query/*.sql`) — add `LIMIT :limit` (documentation only,
   these files aren't executed directly) to `dealer-ledger.sql` (doesn't have one), and add a
   `limit` param row + `LIMIT :limit` to `parts-packing-list.sql` and `warranty-cost.sql`.
2. **CLI runners** (`com.sapreport.dynpro.report.databricks.*QueryRunner`) — add an optional
   trailing `<limit>` arg (default `100` if omitted) to `DealerLedgerQueryRunner`,
   `PartsPackingListQueryRunner`, `WarrantyCostQueryRunner`; append `LIMIT ?` to each query.
3. **Generic framework live query** — `StubReportQueryExecutor.executePartsPackingList`: add
   `LIMIT ?` bound to `request.pageSize()` (already available on `ReportQueryRequest`, defaults
   to the report's `paging.defaultPageSize`), so the live Databricks call can never return more
   rows than the framework is configured to page with. This directly fixes the OOM reproduced
   in testing.
4. **Warranty Cost standalone live-fetch endpoint** — add an optional `limit` field (default
   `100`) to `WarrantyCostRequest`, threaded through `WarrantyCostDatabricksService.fetch(...)`
   and appended as `LIMIT ?` to its query — same shape as `DealerLedgerDatabricksService`.
5. `DealerLedgerDatabricksService`/`DealerLedgerDatabricksController` already have `limit`
   (default 100) — unchanged.

## Out of scope
- No true DB-side cursor/offset pagination (page > 1 still isn't pushed down to Databricks for
  PARTS_PACKING_LIST); this only bounds the single query result size.
- No change to the actual Databricks-side SQL functions' signatures.

## Verification
- `mvn -q compile` succeeds.
- Re-run the previously-OOMing request (`dealerCode: ""`, full-year `packingDate` range) and
  confirm it returns within `pageSize` rows instead of crashing.
- `WarrantyCostDatabricksController` and each CLI runner still work with and without an explicit
  limit.
