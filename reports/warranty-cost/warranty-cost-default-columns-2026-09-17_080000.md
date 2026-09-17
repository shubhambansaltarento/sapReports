# Spec: WARRANTY_COST — trim columns to `dealerCode` / `dealerName` only

## Goal

For now, WARRANTY_COST should expose only `dealerCode` and `dealerName` — not
just as defaults, but as the only columns in the report config. The other
columns (`claimNo`, `claimDate`, `partNo`, `partDescription`, `laborCost`,
`partCost`, `totalCost`) are removed from `columnGroups` entirely; they can be
reintroduced later when the report needs them.

## Change

In `src/main/resources/reports/warranty-cost.json`, `columnGroups[0].columns`
keeps only:

- `dealerCode`
- `dealerName`

`parameters` (`dealerCode`, `companyCode`, `claimDate`) are unchanged — the
`claimDate` search param still drives filtering even though it's no longer a
displayed column.

The stub executor (`StubReportQueryExecutor.executeWarrantyCost`) is unchanged:
it still builds full canned rows (all fields) and still computes SUM totals for
`laborCost`/`partCost`/`totalCost` over the filtered set — `project()` already
trims each row down to `request.effectiveColumns()`, which now resolves to just
`dealerCode`/`dealerName` since that's all the config declares.

## Effect

- `GET /api/v1/reports/WARRANTY_COST/config` — `effectiveColumns` only lists
  `dealerCode`/`dealerName`.
- `POST /api/v1/reports/WARRANTY_COST/data` — each row only contains
  `dealerCode`/`dealerName`; `totals` still reports `laborCost`/`partCost`/
  `totalCost` sums (totals aren't tied to column visibility).

## Out of scope

- No per-request column selection API — columns are config-driven, same as
  DEALER_LEDGER.
- Removed columns aren't deleted from the stub row builder, just from the
  config — reintroducing them later is a config-only change.
