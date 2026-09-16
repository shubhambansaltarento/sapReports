# Dealer Ledger — Pagination Spec

Date: 2026-09-16 17:00:00 IST
Status: **SPEC ONLY** — no implementation changes yet.

## Context

The Angular grid's pagination is not working correctly against the current
`POST /api/v1/reports/DEALER_LEDGER/data` response. Rather than debug/fix server-side paging
right now, the decision is to **stop paginating at the API for DEALER_LEDGER** and let the UI
handle paging entirely client-side, using the full row set returned in one call.

## Decision

- `POST /api/v1/reports/DEALER_LEDGER/data` returns **all rows** for the given filters in a
  single response — no server-side slicing.
- Pagination (page size, current page, next/prev) becomes a **UI-only concern**: the Angular
  grid pages through the already-fetched `rows` array in memory.
- This is a **temporary, report-specific decision** for `DEALER_LEDGER` only, driven by the
  stub data source being small (tens of rows). It is not a general policy for every report.

## What changes

### Request (`ReportDataRequest`)

- `paging` may still be sent by the client but is **ignored** by the server for this report —
  documented as a no-op, not removed from the contract (other reports may still honor it).

### Response (`ReportDataResponse`)

- `rows` contains every row matching `parameters` (no `page`/`pageSize` slicing).
- `paging` in the response still reports `totalRows`, but `page` is always `1` and `pageSize`
  equals `totalRows` (or a large fixed ceiling) so existing clients that read `paging.totalPages`
  don't break — `totalPages` becomes `1`.

Example:

```json
{
  "paging": { "page": 1, "pageSize": 41, "totalRows": 41, "totalPages": 1 }
}
```

## Implementation note (for when this is built)

In `StubReportQueryExecutor.execute(...)` (`src/main/java/com/sapreport/dynpro/report/query/StubReportQueryExecutor.java`),
skip the `fromIndex`/`toIndex` slicing for `DEALER_LEDGER` and return the full `projected` list as
the page. `ReportService.getData(...)` already computes `pagingResponse` from
`result.totalRows()`/`pageSize` — pass `pageSize = result.totalRows()` (or keep the requested
`pageSize` but ensure the full row list is what's actually returned) so `totalPages` comes out to
`1`.

## Out of scope

- Fixing/implementing real server-side pagination for `DEALER_LEDGER` — deferred.
- Any change to other reports' pagination behavior.
- The root cause of why the existing pagination UI wiring breaks on initial load (separate bug,
  not addressed by this decision — this spec only removes server-side paging as a variable).

## Status

**Spec only.** No changes made to `StubReportQueryExecutor`, `ReportService`, or the Angular grid
yet — awaiting confirmation before implementing.
