# Dealer Ledger — Column Schema Gap Check

Date: 2026-09-16 12:57:43 IST
Status: **IMPLEMENTED (2026-09-16 13:00 IST)** — all missing columns added to
`dealer-ledger.json`; field naming below is a best-effort SAP-field mapping and should be
confirmed/renamed once real source columns are wired up (`jdbc-connection.md`).

## Source

Required columns per the report layout supplied (SAP-style dealer ledger extract header row):

```
Dealer Code | Doc. Type | Doc. Reference No. | Doc. Date | Assignment | CCA | Text Dec. |
Narration Veh. Description | Debit Amount | Credit Amount | Dealer Name | Dealer Address |
Currency | Text | QNT. | Amt.
```

Compared against the current schema: `src/main/resources/reports/dealer-ledger.json`
(`reportCode: DEALER_LEDGER`), `columnGroups[].columns`.

## Current schema columns

| field | label |
|---|---|
| `postingDate` | Posting Date |
| `debit` | Debit |
| `credit` | Credit |
| `runningBalance` | Balance |
| `cblRefNo` | CBL Ref No (only when `withCblDetails` is checked) |

## Gap Result

| Required column | Present in schema? | Notes |
|---|---|---|
| Dealer Code | ❌ Missing | Currently only surfaced once in `context.dealerCode` (a header/parameter value), not as a per-row column. Confirm whether it needs to repeat per row or stays header-only. |
| Doc. Type | ❌ Missing | No equivalent field. |
| Doc. Reference No. | ❌ Missing | No equivalent field. |
| Doc. Date | ⚠️ Ambiguous overlap | Schema has `postingDate` ("Posting Date"), not "Doc. Date". In SAP FI these are typically two distinct dates (document date vs. posting date). Need confirmation: is `postingDate` meant to satisfy this column, or is a separate `docDate` field required? |
| Assignment | ❌ Missing | No equivalent field (SAP "Assignment" — `ZUONR`). |
| CCA | ❌ Missing | No equivalent field. Meaning of "CCA" not confirmed (e.g. Cash Collection Account / Cost Center Assignment) — needs clarification. |
| Text Dec. | ❌ Missing | No equivalent field (likely "Text Description" / SAP `SGTXT`). |
| Narration Veh. Description | ❌ Missing | No equivalent field — likely vehicle-related narration text, specific to this report's domain. |
| Debit Amount | ✅ Present | `debit`. |
| Credit Amount | ✅ Present | `credit`. |
| Dealer Name | ❌ Missing | Currently only in `context.dealerDescription` (header-level), not a row column. |
| Dealer Address | ❌ Missing | No equivalent field anywhere in current schema. |
| Currency | ❌ Missing | No equivalent field. |
| Text | ❌ Missing | No equivalent field — distinct from "Text Dec." above; needs clarification on how these two differ. |
| QNT. | ❌ Missing | No equivalent field (quantity — likely relevant only for vehicle/material line items). |
| Amt. | ❌ Missing | No equivalent field. Relationship to existing `debit`/`credit`/`runningBalance` needs clarification — is "Amt." a signed net amount distinct from debit/credit? |

## Extra fields in schema not in the required list

- `runningBalance` ("Balance") — not in the required column list. Keep (useful running total) or drop? Needs confirmation.
- `cblRefNo` ("CBL Ref No", conditional column) — not in the required column list. Keep as-is (an additional detail view) or fold into the base column set? Needs confirmation.

## Open Questions

1. Should `Dealer Code` / `Dealer Name` be per-row columns, or is header-level `context` sufficient (current design)?
2. Is `Doc. Date` the same as the existing `postingDate`, or a genuinely separate field alongside a new `postingDate`-equivalent?
3. Exact meaning/source of `CCA`, `Assignment`, `Text Dec.` vs. `Text`, and `Amt.` vs. `debit`/`credit` — need SAP field mapping (likely `BSEG`/`BSAD`-style fields: `ZUONR` for Assignment, `SGTXT` for text, `DMBTR`/`WRBTR` for amount, `MENGE` for quantity).
4. Should `runningBalance` and `cblRefNo` be retained?

## Resolution (2026-09-16 13:00 IST)

Added all 13 missing columns to the `base` column group in
`src/main/resources/reports/dealer-ledger.json`, alongside the existing `postingDate`, `debit`,
`credit`, `runningBalance`. Field names chosen using standard SAP FI field semantics pending
confirmation:

| field | label | assumed SAP source |
|---|---|---|
| `dealerCode` | Dealer Code | — |
| `docType` | Doc. Type | `BLART` |
| `docReferenceNo` | Doc. Reference No. | `XBLNR` |
| `docDate` | Doc. Date | `BLDAT` (kept distinct from existing `postingDate` = `BUDAT`) |
| `assignment` | Assignment | `ZUONR` |
| `cca` | CCA | unconfirmed — see Open Questions |
| `textDec` | Text Dec. | `SGTXT` (assumed distinct from `text` below — unconfirmed) |
| `vehicleNarration` | Narration Veh. Description | report-specific, no direct SAP field identified |
| `dealerName` | Dealer Name | — |
| `dealerAddress` | Dealer Address | — |
| `currency` | Currency | `WAERS` |
| `text` | Text | unconfirmed — overlaps conceptually with `textDec`, needs disambiguation |
| `qnt` | QNT. | `MENGE` |
| `amt` | Amt. | unconfirmed — kept separate from `debit`/`credit`, needs clarification on when populated |

`Debit`/`Credit` labels were also updated to `Debit Amount`/`Credit Amount` to match the required
header text; field names (`debit`, `credit`) unchanged.

Verified via `./mvnw.cmd test -Dtest=ClasspathReportMetadataRepositoryTest,ReportServiceTest`
(passes — those tests assert group keys, not exact columns) and via a live
`GET /api/v1/reports/DEALER_LEDGER/config` call, confirming all 16 required fields are present in
the response.

**Still open** — same items as above (`CCA` meaning, `Text` vs. `Text Dec.` distinction, `Amt.` vs.
`debit`/`credit` semantics, and whether `runningBalance`/`cblRefNo` should stay) — the schema now
has *placeholders* for every required column, but the underlying data source
(`jdbc-connection.md`, still stubbed) will need real field mappings before this is more than a
shape contract.
