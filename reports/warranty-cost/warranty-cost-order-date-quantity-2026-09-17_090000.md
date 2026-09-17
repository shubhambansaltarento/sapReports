# Spec: WARRANTY_COST — add `orderDate` / `quantity` columns + expand stub data to 20 rows

## Goal

Add two new columns to WARRANTY_COST — `orderDate` and `quantity` — and expand
the stub `/data` dataset from 8 to 20 dummy rows so there's more realistic
volume to exercise paging/sorting against.

## Schema change

In `src/main/resources/reports/warranty-cost.json`, add to `columnGroups[0].columns`:

| Column | Type | Notes |
|---|---|---|
| `orderDate` | date (`dd-MM-yyyy`) | the warranty claim's originating sales order date, precedes `claimDate` |
| `quantity` | integer | qty of parts claimed |

Both are added alongside the existing `dealerCode`/`dealerName` (per
`warranty-cost-default-columns-2026-09-17_080000.md`, currently the only two
columns declared) — so after this change `columnGroups[0].columns` is:
`dealerCode`, `dealerName`, `orderDate`, `quantity`.

## Data change

`StubReportQueryExecutor.buildWarrantyCostRows()` (in
`src/main/java/com/sapreport/dynpro/report/query/StubReportQueryExecutor.java`)
expands from 8 to 20 canned rows, all still dealer `10015` /
"PAWAN SARKAR AUTOMOBILES", spread across the existing May–Aug 2026 claimDate
range so the existing `claimDate` from/to filter in
`executeWarrantyCost` still exercises them. Each row's `orderDate` is a few
days before its `claimDate`; `quantity` is a small dummy int (1-4).

`claimNo`/`partNo`/`laborCost`/`partCost`/`totalCost` stay in the row map (the
executor still builds full rows and totals) even though they're not currently
in the config's column list — `project()` trims to `effectiveColumns()` same
as before.

## Effect

- `GET /api/v1/reports/WARRANTY_COST/config` — `effectiveColumns` now lists
  `dealerCode`, `dealerName`, `orderDate`, `quantity`.
- `POST /api/v1/reports/WARRANTY_COST/data` — 20 rows returned (before paging),
  each with `dealerCode`/`dealerName`/`orderDate`/`quantity`; `totals` still
  reports `laborCost`/`partCost`/`totalCost` sums over the full stub row data.

## Out of scope

- No sorting/formatting changes beyond declaring the two new columns.
- No change to parameters or paging config.
