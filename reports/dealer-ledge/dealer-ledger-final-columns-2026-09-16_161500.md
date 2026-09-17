# Dealer Ledger — Final Column Set (Trim Spec)

Date: 2026-09-16 16:15:00 IST
Status: **SPEC ONLY** — no implementation changes yet.

## Source

New screenshot (Excel export of the ledger, rows 93–99) supplied 2026-09-16, showing the exact
column header row the report must produce:

```
Dealer Code | Doc. Type | Doc. Reference No. | Doc. Date | Assignment | CCA | Text Dec. |
Narration Veh. Description | Debit Amount | [Credit Amount, off-screen — same pattern as the
earlier screenshot]
```

Sample rows visible in the screenshot:

| Dealer Code | Doc. Type | Doc. Reference No. | Doc. Date | Assignment | CCA | Text Dec. | Narration Veh. Description | Debit Amount |
|---|---|---|---|---|---|---|---|---|
| 10015 | | | | | Z3W1 | | 3 WHEELER VEHICLE CLOSING BALANCE | |
| 10015 | | | | | Z3W2 | | 3 WHEELER SPARES ACCOUNT OPENING BALANCE | |
| 10015 | | | | | Z3W2 | | 3 WHEELER SPARES ACCOUNT TOTAL | |
| 10015 | | | | | Z3W2 | | 3 WHEELER SPARES CLOSING BALANCE | |
| 10015 | | | | | ZEVC | | MISCELLANEOUS OPENING BALANCE | |
| 10015 | RV | 90885569 | 8/16/2026 | 0092486496 | ZEVC | | TVS ORBITER V2 - COSMIC TITANIUM | 681,345.00 |
| 10015 | RV | 90885570 | 8/16/2026 | 1501116245 | ZEVC | | TVS iQUBE ELECTRIC SMARTXONNECT PR PW | 919,500.75 |

Notes from this sample:

- `CCA` takes more values than previously seen (`ZTS1`, now also `Z3W1`, `Z3W2`, `ZEVC`) —
  appears to be a per-vehicle-category cost/account segment (e.g. 3-wheeler, EV), not a
  single fixed value.
- Balance/total narration rows (closing/opening/total) again have no `Doc. Type` /
  `Doc. Reference No.` / `Doc. Date` / `Assignment` / amounts — same as the dealer/vehicle
  opening-balance rows in the first screenshot.
- `Text Dec.` is blank on every visible row here; narration lands in
  `Narration Veh. Description` instead — consistent with the ambiguity already flagged in
  `docs/spec/reports/dealer-ledger-column-gap-2026-09-16_125743.md`.

## Requested change

Keep **only** the fields shown in this screenshot's header row (plus `Credit Amount`, which
continues the row off-screen the same way it did in the previous screenshot) — remove every
other field currently in the `base` column group of `dealer-ledger.json`.

## Field decision

| API field | Label | Keep? | Reason |
|---|---|---|---|
| `dealerCode` | Dealer Code | ✅ Keep | In header |
| `docType` | Doc. Type | ✅ Keep | In header |
| `docReferenceNo` | Doc. Reference No. | ✅ Keep | In header |
| `docDate` | Doc. Date | ✅ Keep | In header |
| `assignment` | Assignment | ✅ Keep | In header |
| `cca` | CCA | ✅ Keep | In header |
| `textDec` | Text Dec. | ✅ Keep | In header |
| `vehicleNarration` | Narration Veh. Description | ✅ Keep | In header |
| `debit` | Debit Amount | ✅ Keep | In header |
| `credit` | Credit Amount | ✅ Keep | Off-screen, same as prior screenshot's pattern |
| `postingDate` | Posting Date | ❌ Remove | Not in header; note `postingDate` is also a required *search parameter* — removing it as a **column** does not remove the search filter |
| `dealerName` | Dealer Name | ❌ Remove | Not in header |
| `dealerAddress` | Dealer Address | ❌ Remove | Not in header |
| `currency` | Currency | ❌ Remove | Not in header |
| `text` | Text | ❌ Remove | Not in header |
| `qnt` | QNT. | ❌ Remove | Not in header |
| `amt` | Amt. | ❌ Remove | Not in header |
| `runningBalance` | Balance | ❌ Remove | Not in header |
| `cblRefNo` (in `cbl` group) | CBL Ref No | ⚠️ Needs confirmation | Not in this header either, but it's a separate conditional column group (`withCblDetails`) — unclear if it should also be dropped or is intentionally out of scope of this trim |

## Resulting `base` column list (proposed)

```
dealerCode, docType, docReferenceNo, docDate, assignment, cca, textDec, vehicleNarration, debit, credit
```

10 columns, down from the current 18.

## Impact of implementing this

- `dealer-ledger.json` `base` column group trimmed to the 10 fields above; `configVersion` must
  bump again (currently `2026.09.1`).
- `StubReportQueryExecutor` row builders (`balanceRow`, `docRow`) drop the now-unused fields
  (`dealerName`, `dealerAddress`, `currency`, `text`, `qnt`, `amt`, `runningBalance`,
  `postingDate` as a row value) — `postingDate` stays as a query **parameter**, just not echoed
  back as a row column.
- `reports/dealer-ledge/spec.md` (the original full-field spec) becomes stale and should be
  superseded by this trimmed spec once implemented.
- Existing tests asserting `effectiveColumns` contains `postingDate`/`runningBalance`
  (`ReportServiceTest.columnGroupToggling_hidesCblColumnWhenFlagFalse`) will need updating.

## Open question

Should `cblRefNo` / the `cbl` column group be removed too, or kept as-is since it wasn't part of
either screenshot's visible header (it's a conditional detail column, not part of the base grid)?

## Status

**Spec only.** No changes made to `dealer-ledger.json`, `StubReportQueryExecutor`, or tests yet —
awaiting confirmation before implementing, per the trim requested.
