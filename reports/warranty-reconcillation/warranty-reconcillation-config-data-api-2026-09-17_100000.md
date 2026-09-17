# Spec: WARRANTY_RECONCILLATION — config + `/data` API

New report, following the same pattern as WARRANTY_COST
(`reports/warranty-cost/warranty-cost-data-api-2026-09-17_140000.md`): a
classpath-loaded JSON config (auto-served by the generic
`GET /api/v1/reports/WARRANTY_RECONCILLATION/config`) plus a stub branch in
`StubReportQueryExecutor` for `POST /api/v1/reports/WARRANTY_RECONCILLATION/data`,
since there's no Databricks-backed executor yet.

## Endpoints

| Purpose | Method | Path |
|---|---|---|
| Metadata (params/columns) | GET | `/api/v1/reports/WARRANTY_RECONCILLATION/config` |
| Row data | POST | `/api/v1/reports/WARRANTY_RECONCILLATION/data` |

## Config

New file `src/main/resources/reports/warranty-reconcillation.json`, modeled on
`warranty-cost.json`:

- `reportCode`: `WARRANTY_RECONCILLATION`
- `configVersion`: `2026.09.1`
- `parameters`: `dealerCode` (optional text), `companyCode` (required select,
  default `TVSL`) — same shape as WARRANTY_COST, no date-range param for now
  since this report isn't scoped to a date filter yet.
- `columnGroups`: a single `base` group with just the two columns requested —
  `dealerCode`, `dealerName` — same as WARRANTY_COST's current trimmed set
  (`warranty-cost-default-columns-2026-09-17_080000.md`).
- `export`: `{ "formats": ["XLSX", "PDF"] }`
- `paging`: `{ "defaultPageSize": 50, "maxPageSize": 1000 }`

## Data (stub)

`StubReportQueryExecutor` gets a new `WARRANTY_RECONCILLATION` branch,
dispatched the same way as `WARRANTY_COST`:

- Canned rows for dealer `10015` / "PAWAN SARKAR AUTOMOBILES" (same stub
  dealer as DEALER_LEDGER/WARRANTY_COST, per `StubReportCallerContext.java`).
- Filter by `dealerCode` request parameter (blank/absent means no filter).
- Project rows down to `request.effectiveColumns()` via the existing `project`
  helper.
- No aggregate/totals columns declared yet, so `totals` is an empty map (same
  as DEALER_LEDGER's balance-only totals shape, but here just `Map.of()`).
- Paging: normal page/pageSize slicing (same as WARRANTY_COST), since there's
  no UI-controlled pagination requirement for this report.

## Out of scope

- No real Databricks/SAP-backed executor — stub/dummy data only.
- No actions, lookups, or documents.
- No claim/date filtering yet — can be added later like WARRANTY_COST's
  `claimDate` param if the report needs it.
