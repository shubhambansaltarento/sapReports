# Spec: WARRANTY_COST — `/data` API

Config already exists (`src/main/resources/reports/warranty-cost.json`, auto-served by the
generic `GET /api/v1/reports/WARRANTY_COST/config`). This spec covers wiring up
`POST /api/v1/reports/WARRANTY_COST/data`, following the DEALER_LEDGER stub pattern
(`StubReportQueryExecutor.java`) since there's no Databricks-backed executor yet.

## Endpoints

| Purpose | Method | Path |
|---|---|---|
| Metadata (params/columns) | GET | `/api/v1/reports/WARRANTY_COST/config` |
| Row data | POST | `/api/v1/reports/WARRANTY_COST/data` |

## Schema (unchanged from config)

`dealerCode` is a column like in DEALER_LEDGER — it comes back on every row (not just a
filter param), sourced from the stub caller context (dealer `10015`, same as DEALER_LEDGER's
stub data — `StubReportCallerContext.java`).

| Column | Type | Notes |
|---|---|---|
| `dealerCode` | string | |
| `dealerName` | string | |
| `claimNo` | string | |
| `claimDate` | date (`dd-MM-yyyy`) | required search param, also a row column |
| `partNo` | string | |
| `partDescription` | string | |
| `laborCost` | decimal | SUM aggregate |
| `partCost` | decimal | SUM aggregate |
| `totalCost` | decimal | SUM aggregate |

## Request

```json
{
  "parameters": {
    "dealerCode": "10015",
    "companyCode": "TVSL",
    "claimDate": { "from": "2026-05-01", "to": "2026-08-31" }
  },
  "configVersion": "2026.09.1"
}
```

## Implementation

`StubReportQueryExecutor.execute` currently short-circuits to empty results for any
`reportCode` other than `DEALER_LEDGER`. Add a `WARRANTY_COST` branch:

- A small set of canned dummy rows (dealer `10015`, `dealerName` "PAWAN SARKAR AUTOMOBILES"),
  each with `claimNo`/`claimDate`/`partNo`/`partDescription`/`laborCost`/`partCost`, and
  `totalCost` computed as `laborCost + partCost`.
- Filter by `dealerCode` request parameter the same way DEALER_LEDGER does (blank/absent
  means no filter).
- Project rows down to `request.effectiveColumns()` via the existing `project` helper.
- Totals: `SUM` of `laborCost`, `partCost`, `totalCost` across the filtered rows (matches
  the `aggregate: "SUM"` columns in `warranty-cost.json`).
- Paging: normal page/pageSize slicing (unlike DEALER_LEDGER, which is UI-controlled per
  `dealer-ledger-pagination-2026-09-16_170000.md) — WARRANTY_COST has no such requirement
  yet, so `ReportService`'s existing page/pageSize clamping applies as-is.

## Out of scope

- No real Databricks/SAP-backed executor — this is stub/dummy data only, same caveat as
  DEALER_LEDGER's current implementation.
- No actions, lookups, or documents for WARRANTY_COST yet.
