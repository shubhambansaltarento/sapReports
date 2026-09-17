# Spec: WARRANTY_RECONCILLATION — add from/to date range param

## Goal

The `/data` API for WARRANTY_RECONCILLATION should accept a from/to date
range, same shape as WARRANTY_COST's `claimDate` (`warranty-cost-data-api-
2026-09-17_140000.md`), superseding the "no date-range param for now" note in
`warranty-reconcillation-config-data-api-2026-09-17_100000.md`.

## Config change

In `src/main/resources/reports/warranty-reconcillation.json`, add a
`reconciliationDate` parameter:

```json
{
  "name": "reconciliationDate",
  "label": "Reconciliation Date",
  "control": "DATE_RANGE",
  "dataType": null,
  "required": true,
  "validation": {
    "minDate": "2020-04-01",
    "maxDate": "$TODAY",
    "maxRangeDays": 366,
    "requireBothBounds": true
  },
  "layout": { "row": 2, "span": 6 }
}
```

Add a matching `reconciliationDate` column (date, `dd-MM-yyyy`) to
`columnGroups[0].columns` so it's visible on rows, same as WARRANTY_COST shows
`claimDate` as both a required search param and a row column.

## Data change

`StubReportQueryExecutor.executeWarrantyReconcillation` filters rows by the
`reconciliationDate` request param's `from`/`to` bounds, same pattern as
`executeWarrantyCost`'s `claimDateBound`/`claimDate` filtering (reuse the
existing `claimDateBound` helper, generalized, or a copy scoped to this
report). Expand `buildWarrantyReconcillationRows()` from the single dummy row
to a handful of dummy rows (dealer `10015`) spread across a date range so the
filter is actually exercisable.

## Effect

- `GET /api/v1/reports/WARRANTY_RECONCILLATION/config` — `reconciliationDate`
  appears as a required DATE_RANGE parameter and as a row column.
- `POST /api/v1/reports/WARRANTY_RECONCILLATION/data` — rows are filtered to
  those whose `reconciliationDate` falls within the requested `from`/`to`
  range (inclusive), in addition to the existing `dealerCode` filter.

## Out of scope

- No aggregate/totals columns — still `Map.of()`.
- No change to paging behavior (still normal page/pageSize slicing).
