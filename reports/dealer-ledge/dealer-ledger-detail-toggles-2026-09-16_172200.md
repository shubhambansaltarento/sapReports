# Dealer Ledger — Additional Detail Toggle Params

Date: 2026-09-16 17:22:00 IST
Status: **IMPLEMENTED** (see "Implementation" section) — written after the fact per instruction
to spec first going forward.

## Source

UI screenshot showing an "Include Details" control group with 5 checkboxes beyond the existing
`With CBL Details`:

```
Include Details (2 selected)
[ ] With OE Details   [x] With SP Details   [x] With AC Details   [ ] With EV Details   [ ] With ACWSH Details
```

The Angular app submits these as request parameters the current `dealer-ledger.json` schema
didn't define, which is why calls including them were failing (unknown-parameter validation).

## Decision

Mirror the existing `withCblDetails` → `cbl` column-group pattern for each new toggle: a
`CHECKBOX`/`BOOLEAN` parameter plus a conditionally-visible column group with a single
`<prefix>RefNo` column, since — like CBL — the exact real columns per detail type aren't known
yet.

| Toggle | Parameter name | Column group key | Column field | Label |
|---|---|---|---|---|
| With OE Details | `withOeDetails` | `oe` | `oeRefNo` | OE Ref No |
| With SP Details | `withSpDetails` | `sp` | `spRefNo` | SP Ref No |
| With AC Details | `withAcDetails` | `ac` | `acRefNo` | AC Ref No |
| With EV Details | `withEvDetails` | `ev` | `evRefNo` | EV Ref No |
| With ACWSH Details | `withAcwshDetails` | `acwsh` | `acwshRefNo` | ACWSH Ref No |

All five are optional booleans (`required: false`, `defaultValue: false`), same as
`withCblDetails` — so omitting them from a request is safe.

## Implementation

- `src/main/resources/reports/dealer-ledger.json` — added the 5 parameters and 5 column groups
  above; bumped `configVersion` to `2026.09.3`.
- `src/main/java/com/sapreport/dynpro/report/query/StubReportQueryExecutor.java` — added
  `oeRefNo`/`spRefNo`/`acRefNo`/`evRefNo`/`acwshRefNo` (all `null`) to every stub row, same
  treatment as `cblRefNo`.
- Updated `ReportServiceTest`/`ClasspathReportMetadataRepositoryTest` for the new config
  version, parameter list, and column group list.

## Open items

1. **Real column names unconfirmed** — `oeRefNo`/`spRefNo`/`acRefNo`/`evRefNo`/`acwshRefNo` are
   placeholders (same caveat as `cblRefNo`); the actual SAP-side field(s) per detail type aren't
   known yet — likely each detail type needs more than a single ref-no column once real data is
   wired up.
2. What do `OE` / `SP` / `AC` / `EV` / `ACWSH` stand for exactly, in this report's business
   context? (Guesses: OE = Original Equipment, SP = Spare Parts, AC = Accessories, EV =
   Electric Vehicle, ACWSH = Accessories Workshop — unconfirmed.)
3. Should any of these toggles be mutually exclusive, or can any combination be selected
   together (screenshot shows 2 selected simultaneously, so current implementation allows any
   combination — matches the UI)?

## Status

Implemented per the pattern above; not yet pushed. Full test suite last run before this note was
passing at 80/80 (pre-existing suite + this change). Next step if you want to proceed differently:
revert to spec-only and confirm items 1–3 above before implementing.
