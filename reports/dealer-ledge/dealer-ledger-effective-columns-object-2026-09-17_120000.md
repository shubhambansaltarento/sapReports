# Spec: `effectiveColumns` as objects, not plain strings

API: `POST /api/v1/reports/DEALER_LEDGER/data`

## Problem

`ReportDataResponse.effectiveColumns` (`ReportDataResponse.java:10`) is currently
`List<String>` — just the field names computed by `ReportService.computeEffectiveColumns`
(`ReportService.java:147-157`) from the visible column groups. The UI has no way to tell,
from this field alone, which columns should be checked/shown by default in a column picker
vs. which are available but hidden by default.

## Current shape

```json
"effectiveColumns": [
  "dealerCode",
  "docType",
  "docReferenceNo",
  "docDate",
  "assignment",
  "cca",
  "textDec",
  "vehicleNarration",
  "debit",
  "credit"
]
```

## Target shape

```json
"effectiveColumns": [
  { "columnName": "dealerCode", "isDefault": true },
  { "columnName": "docType", "isDefault": true },
  { "columnName": "docReferenceNo", "isDefault": true },
  { "columnName": "docDate", "isDefault": true },
  { "columnName": "assignment", "isDefault": true },
  { "columnName": "cca", "isDefault": true },
  { "columnName": "textDec", "isDefault": false },
  { "columnName": "vehicleNarration", "isDefault": false },
  { "columnName": "debit", "isDefault": true },
  { "columnName": "credit", "isDefault": true }
]
```

Per the requirement, for `DEALER_LEDGER`'s `base` group: every column defaults to
`isDefault: true` **except** `textDec` and `vehicleNarration`, which are `isDefault: false`.
Columns from the conditional detail groups (`cbl`, `oe`, `sp`, `ac`, `ev`, `acwsh`) only
appear in `effectiveColumns` when their `visibleWhen` flag is set on the request; when
present, treat them as `isDefault: true` (no conditional-group column is currently called
out as non-default).

## Implementation notes

- New type `EffectiveColumn(String columnName, boolean isDefault)` (or reuse a similar
  record if one already exists) replacing the raw string element type.
- `ReportDataResponse.effectiveColumns` (`ReportDataResponse.java:10`): change
  `List<String>` → `List<EffectiveColumn>`.
- `isDefault` needs a source of truth per column. Since `ColumnDefinition`
  (`ColumnDefinition.java:12-25`) has no existing "default visible" flag, add one there
  (e.g. `boolean defaultVisible`, defaulting to `true` via the compact constructor like
  `columnType` already does) and set it to `false` for `textDec`/`vehicleNarration` in
  `dealer-ledger.json` (and any other report's metadata where the same distinction applies).
- `ReportService.computeEffectiveColumns` (`ReportService.java:147-157`): change return
  type to `List<EffectiveColumn>`, emitting `new EffectiveColumn(column.field(), column.defaultVisible())`
  instead of `columns.add(column.field())`.
- `SortValidator.validate(sort, effectiveColumns, columnsByField)` (`ReportService.java:84`)
  currently takes `List<String>` — update it to read `columnName` from the new type
  (or extract a `List<String>` of names before calling it, whichever keeps the diff smaller).
- Any test asserting `effectiveColumns` as a flat string list (`ReportServiceTest.java`)
  needs updating to the new object shape.

## Client impact

Any caller reading `effectiveColumns` as a flat list of strings (Angular frontend, tests,
Postman collections) must switch to reading `.columnName` per entry, and can use `.isDefault`
to drive the initial column-picker selection.

## Out of scope

- No change to `columnGroups`/`ColumnGroup` shape in `ReportConfigResponse` — this only
  affects the per-call `effectiveColumns` field in `ReportDataResponse`.
- No change to which columns are computed as effective (visibility-by-`visibleWhen` logic
  is unchanged) — only how each is represented.

## Addendum: add `isVisible`

Add a third key, `isVisible`, alongside `columnName`/`isDefault`.

For `DEALER_LEDGER` right now: the `base` group's columns (the ones shown in the reference
screenshot — Dealer Code, Doc. Type, Doc. Reference No., Doc. Date, Assignment, CCA,
Text Dec., Narration Veh. Description, Debit Amount, Credit Amount) are `isVisible: true`.
Every other column — the conditional detail columns (`cblRefNo`, `oeRefNo`, `spRefNo`,
`acRefNo`, `evRefNo`, `acwshRefNo`) — is `isVisible: false`, even when their group's
`visibleWhen` flag is set on the request and the column appears in `effectiveColumns`.

```json
"effectiveColumns": [
  { "columnName": "dealerCode", "isDefault": true, "isVisible": true },
  { "columnName": "docType", "isDefault": true, "isVisible": true },
  { "columnName": "docReferenceNo", "isDefault": true, "isVisible": true },
  { "columnName": "docDate", "isDefault": true, "isVisible": true },
  { "columnName": "assignment", "isDefault": true, "isVisible": true },
  { "columnName": "cca", "isDefault": true, "isVisible": true },
  { "columnName": "textDec", "isDefault": false, "isVisible": true },
  { "columnName": "vehicleNarration", "isDefault": false, "isVisible": true },
  { "columnName": "debit", "isDefault": true, "isVisible": true },
  { "columnName": "credit", "isDefault": true, "isVisible": true },
  { "columnName": "cblRefNo", "isDefault": true, "isVisible": false }
]
```

### Implementation notes

- `ColumnDefinition` gets a second flag, `visible` (`Boolean`, same null→`true`-default
  pattern as `defaultVisible`).
- `dealer-ledger.json`: set `"visible": false` on the `cbl`/`oe`/`sp`/`ac`/`ev`/`acwsh`
  groups' columns (`cblRefNo`, `oeRefNo`, `spRefNo`, `acRefNo`, `evRefNo`, `acwshRefNo`);
  leave the `base` group columns unset (defaults to `true`).
- `EffectiveColumn` record gains `isVisible`: `EffectiveColumn(String columnName, boolean isDefault, boolean isVisible)`.
- `ReportService.computeEffectiveColumns` emits `column.visible()` as the third arg.
