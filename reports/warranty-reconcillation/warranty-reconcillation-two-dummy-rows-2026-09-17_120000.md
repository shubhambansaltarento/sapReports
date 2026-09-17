# Spec: WARRANTY_RECONCILLATION — trim stub data to 2 dummy rows

## Goal

Reduce `buildWarrantyReconcillationRows()` (added in
`warranty-reconcillation-date-range-2026-09-17_110000.md`) from 6 dummy rows
down to 2, for a lighter-weight stub dataset.

## Change

`StubReportQueryExecutor.buildWarrantyReconcillationRows()` keeps only 2 rows,
both dealer `10015`, with distinct `reconciliationDate` values so the
`reconciliationDate` from/to filter still has something to exercise.

## Effect

`POST /api/v1/reports/WARRANTY_RECONCILLATION/data` returns at most 2 rows
(fewer if the `dealerCode`/`reconciliationDate` filters exclude one).

## Out of scope

- No change to WARRANTY_COST's dataset (still 20 rows).
- No change to config/columns.
