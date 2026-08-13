# Dynpro — Report Components: Common Design

Status: **DRAFT — common structure only.** Per your instruction, report-wise specs (exact search
parameters, output columns, source query, required role) will be added per report on top of this shared
design — this document defines the shape they'll each fill in.

---

## 1. How this maps onto the already-designed generic APIs

This closes out two open items from `SPEC.md` (§8.2, §8.3): the "8 places" for the generic Get Detail
API and the "8 places" for the generic Search Parameters API **are the reports listed below** — i.e.
`detailType` and `searchType` are now the report keys, and they do map 1:1, confirming the assumption
`SPEC.md` §8.3 flagged as unconfirmed.

- `POST /api/v1/search-parameters` with `searchType = <REPORT_KEY>` → returns that report's filter
  definitions (§3 below), sourced from its config.
- `POST /api/v1/details` with `detailType = <REPORT_KEY>` and `parameters = { ...search values... }` →
  runs that report and returns its data, shaped as this document's common Report Result contract (§4).

Each report also carries a **required role** used by the JWT authorization layer (`jwt-auth.md` §5) —
supplied per report once that report's spec is written.

---

## 2. Report list

| # | Report Key (proposed) | Display Name | Notes |
|---|---|---|---|
| 1 | `DEALER_LEDGER` | Dealer Ledger | |
| 2 | `BILLING` | Billing | |
| 3 | `GOODS_ACKNOWLEDGEMENT` | Goods Acknowledgement | |
| 4 | `WARRANTY_RECONCILIATION` | Warranty Reconciliation | |
| 5 | `WARRANTY_LABOUR_TAX_INVOICE_REPORT` | Warranty Labour Tax Invoice — Report | Split #1 of "Warranty Labour Tax Invoice" |
| 6 | `WARRANTY_LABOUR_ORDER_REPORT` | Warranty Labour Tax Invoice — Order Report | Split #2 of "Warranty Labour Tax Invoice" |
| 7 | `WARRANTY_COST_REPORT` | Warranty Cost Report | |
| 8 | `PARTS_PACKING_LIST` | Parts Packing List | |
| 9 | `VOR_PRINT` | VOR Print | Expansion of "VOR" TBD |
| 10 | `PQM` | PQM | Expansion of acronym TBD |

**Count note**: you said "8 components for 8 reports," but the named list has 9 distinct top-level
reports, and splitting "Warranty Labour Tax Invoice" into its two sub-reports (#5/#6) brings the total
to 10. Flagging this so the enum below gets locked against a confirmed list rather than the count in the
original message — happy to drop/merge/rename any of these once you confirm.

`detailType` / `searchType` enum values (replacing the `DETAIL_TYPE_1..8` / `SEARCH_TYPE_1..8`
placeholders in `openapi.yaml`) are now the "Report Key" column above.

---

## 3. Common "Report Config" shape (per report)

Design-only for now — storage mechanism (static file vs. database table) is an open question in
`jdbc-connection.md` §3.2. Conceptual shape, regardless of where it ends up living:

```yaml
reportKey: DEALER_LEDGER
displayName: Dealer Ledger
description: ...                       # TBD per report

searchParameters:                      # → feeds POST /api/v1/search-parameters response (SPEC.md §4.4)
  - field: dealerCode
    label: Dealer Code
    type: TEXT
    required: true
  - field: fromDate
    label: From Date
    type: DATE
    required: true
  - field: toDate
    label: To Date
    type: DATE
    required: true
  - field: status
    label: Status
    type: SELECT
    optionsSource: STATIC | DB_QUERY   # static list vs. a dropdown populated from a DB lookup
    options: [ ... ]                  # if STATIC

dataSource: REPORTING                  # which JDBC connection this report uses (jdbc-connection.md)
query:
  type: SQL | STORED_PROCEDURE
  reference: dealer-ledger.sql         # or a stored procedure name

resultContract: <per-report row shape> # → feeds the `data` payload in POST /api/v1/details (§4 below)

authorization:
  requiredRoles: [ ... ]               # ties into jwt-auth.md §5 — TBD per report

caching:
  enabled: true | false
  ttlSeconds: 300                      # TBD per report — some reports may be real-time, others cacheable
```

Everything under `searchParameters`, `query`, `resultContract`, and `authorization.requiredRoles` is
**TBD per report** — this is exactly what the report-wise specs you're about to add will fill in.

---

## 4. Common "Report Result" contract

To keep the generic `/details` endpoint genuinely reusable across all reports (rather than each report
inventing its own ad hoc shape), the `data` payload for every report follows one generic tabular
contract, with each report supplying its own `columns`:

```json
{
  "reportKey": "DEALER_LEDGER",
  "generatedAt": "2026-08-13T10:15:30Z",
  "parameters": { "dealerCode": "D123", "fromDate": "2026-01-01", "toDate": "2026-08-01" },
  "columns": [
    { "key": "txnDate", "label": "Transaction Date", "type": "DATE" },
    { "key": "particulars", "label": "Particulars", "type": "TEXT" },
    { "key": "debit", "label": "Debit", "type": "NUMBER" },
    { "key": "credit", "label": "Credit", "type": "NUMBER" },
    { "key": "balance", "label": "Balance", "type": "NUMBER" }
  ],
  "rows": [ { "txnDate": "2026-01-05", "particulars": "...", "debit": 0, "credit": 1500, "balance": 1500 } ],
  "totalRows": 1,
  "page": 0,
  "pageSize": 50
}
```

This gives the External UI one generic grid/table component it can reuse across all 8+ reports, driven
by each response's own `columns` array, rather than needing report-specific rendering logic for every
screen.

**Open question — export formats**: reports commonly need PDF/Excel/CSV export, not just on-screen
tables. Not mentioned yet — flagging since it affects the contract (would likely be a separate endpoint,
e.g. `GET /api/v1/details/export?...&format=pdf`, rather than overloading the JSON contract above).
Confirm if/which export formats are needed, and for which reports.

**Open question — pagination**: `page`/`pageSize`/`totalRows` are included above as a proposal
(`SPEC.md` §5 currently has no pagination convention defined) — confirm this is wanted, and what a
sensible default/max page size is, once real data volumes per report are known.

---

## 5. `ReportComponent` architecture (conceptual — no code yet)

This is the concrete meaning of "component" in "8 components for 8 reports":

- A `ReportComponent` interface, conceptually:
  - `List<FilterDefinition> getSearchParameters()`
  - `ReportResult generate(Map<String, Object> parameters, CallerContext caller)`
- **One implementation per report key** (10, per §2's list), each knowing its own query
  (`jdbc-connection.md` §5) and its own result-column mapping.
- A registry/lookup keyed by the `reportKey` enum, so the generic `/details` and `/search-parameters`
  controllers just resolve the right component and delegate — no per-report branching logic in the
  controllers themselves, and no need to fall back to the 8-discrete-endpoints alternative
  `SPEC.md` §4.3 flagged as a possibility.
- `caller` carries the JWT's `dealerCode`/`roles` (`jwt-auth.md` §2), used both for the authorization
  check (§3's `authorization.requiredRoles`) and potentially to scope the query itself (e.g. a dealer
  should only ever see their own ledger, not another dealer's — **open question**: is that scoping
  rule true for all 10 reports, or do some allow broader access, e.g. for admin/regional roles?).

---

## 6. What's still needed per report (arriving as "report-wise specs")

For each of the 10 entries in §2:

1. Exact search parameters — fields, types, validation rules, dropdown sources.
2. Exact output columns and their types (the `columns` array in §4).
3. Underlying SQL / stored procedure and source table(s) (`jdbc-connection.md`).
4. Required role(s) for access (`jwt-auth.md` §5).
5. Whether this specific report needs export (PDF/Excel/CSV) and/or pagination.
6. Whether results should be scoped to the caller's own dealer, or broader for some roles.
7. Caching behavior — real-time data only, or safe to cache briefly (and for how long).
