# Dealer Ledger — Data API Spec

Source: SAP-style Dealer Ledger extract (screenshot supplied 2026-09-16), used to confirm the
exact field set and sample values the data API (`POST /api/v1/reports/DEALER_LEDGER/data`) must
return.

## Endpoints

| Purpose | Method | Path |
|---|---|---|
| Metadata (params/columns) | GET | `/api/v1/reports/DEALER_LEDGER/config` |
| Row data | POST | `/api/v1/reports/DEALER_LEDGER/data` |

## Request (`POST /data`)

```json
{
  "parameters": {
    "dealerCode": "10015",
    "companyCode": "TVSL",
    "postingDate": { "from": "2026-05-01", "to": "2026-08-31" },
    "withCblDetails": false
  },
  "configVersion": "2026.09.1"
}
```

## Fields (per row)

These map 1:1 to `base` columns in `src/main/resources/reports/dealer-ledger.json`.

| Column header (source extract) | API field | Type | Notes |
|---|---|---|---|
| Dealer Code | `dealerCode` | string | |
| Doc. Type | `docType` | string | e.g. `DR`, `DA`, `RV` |
| Doc. Reference No. | `docReferenceNo` | string | |
| Doc. Date | `docDate` | date (`dd-MM-yyyy`) | |
| Posting Date | `postingDate` | date (`dd-MM-yyyy`) | required search param, also a row column |
| Assignment | `assignment` | string | |
| CCA | `cca` | string | e.g. `ZTS1` |
| Text Dec. | `textDec` | string | |
| Narration Veh. Description | `vehicleNarration` | string | |
| Debit Amount | `debit` | decimal | SUM aggregate |
| Credit Amount | `credit` | decimal | SUM aggregate, source shows trailing `-` for negative (e.g. `862,320.82-`) |
| Dealer Name | `dealerName` | string | |
| Dealer Address | `dealerAddress` | string | |
| Currency | `currency` | string | |
| Text | `text` | string | |
| QNT. | `qnt` | decimal | |
| Amt. | `amt` | decimal | |
| Balance (running) | `runningBalance` | decimal | not in source header, kept as computed running total |
| CBL Ref No | `cblRefNo` | string | only when `withCblDetails=true` |

## Sample response (`ReportDataResponse`)

Rows below reproduce the first entries from the supplied extract for dealer `10015`
(opening balances have no `docType`/`docReferenceNo`; credit shown as negative per source
`862,320.82-` convention → represented as `-862320.82` in JSON).

```json
{
  "reportCode": "DEALER_LEDGER",
  "configVersion": "2026.09.1",
  "effectiveColumns": [
    "dealerCode", "docType", "docReferenceNo", "docDate", "postingDate",
    "assignment", "cca", "textDec", "vehicleNarration", "debit", "credit",
    "dealerName", "dealerAddress", "currency", "text", "qnt", "amt", "runningBalance"
  ],
  "rows": [
    {
      "dealerCode": "10015",
      "docType": null,
      "docReferenceNo": null,
      "docDate": null,
      "postingDate": null,
      "assignment": null,
      "cca": null,
      "textDec": "DEALER OPENING BALANCE",
      "vehicleNarration": null,
      "debit": 0,
      "credit": 0,
      "runningBalance": 0
    },
    {
      "dealerCode": "10015",
      "docType": null,
      "docReferenceNo": null,
      "docDate": null,
      "postingDate": null,
      "assignment": null,
      "cca": "ZTS1",
      "textDec": "VEHICLE ACCOUNT OPENING BALANCE",
      "vehicleNarration": null,
      "debit": 0,
      "credit": -862320.82,
      "runningBalance": -862320.82
    },
    {
      "dealerCode": "10015",
      "docType": "DR",
      "docReferenceNo": "1800367456",
      "docDate": "29-05-2026",
      "postingDate": "29-05-2026",
      "assignment": "20260529",
      "cca": "ZTS1",
      "textDec": null,
      "vehicleNarration": "20260529- FALLP Outstanding Transfer",
      "debit": 5877.89,
      "credit": 0,
      "runningBalance": -856442.93
    },
    {
      "dealerCode": "10015",
      "docType": "DR",
      "docReferenceNo": "1800367459",
      "docDate": "06-01-2026",
      "postingDate": "06-01-2026",
      "assignment": "20260601",
      "cca": "ZTS1",
      "textDec": null,
      "vehicleNarration": "20260601- FALLP Outstanding Transfer",
      "debit": 711.76,
      "credit": 0,
      "runningBalance": -855731.17
    },
    {
      "dealerCode": "10015",
      "docType": "RV",
      "docReferenceNo": "90885573",
      "docDate": "16-08-2026",
      "postingDate": "16-08-2026",
      "assignment": "1501116247",
      "cca": "ZTS1",
      "textDec": "TVS RAIDER - OBDIIB DISC SS ES+KS M.GREY",
      "vehicleNarration": null,
      "debit": 981172.91,
      "credit": 0,
      "runningBalance": null
    }
  ],
  "totals": {
    "debit": 1000000.00,
    "credit": -862320.82
  },
  "paging": { "page": 1, "pageSize": 50, "totalRows": 5, "totalPages": 1 },
  "meta": { "generatedAt": "2026-09-16T09:00:00Z", "dataAsOf": "2026-09-16T09:00:00Z", "queryMs": 12 }
}
```

## Open items carried over from column-gap analysis

See `docs/spec/reports/dealer-ledger-column-gap-2026-09-16_125743.md`:

- `CCA` exact business meaning unconfirmed.
- `Text` vs `Text Dec.` distinction unconfirmed — extract shows only one text-like value per row
  landing in either `textDec` (opening balances, item descriptions) or `vehicleNarration`
  (transfer narrations); real source mapping for both `text` and `textDec` needs SAP-side
  confirmation.
- `Amt.` vs `debit`/`credit`/`runningBalance` semantics unconfirmed — not present in the sample
  extract rows seen so far.
- Opening-balance rows have no `docType`/`docReferenceNo`/dates — API must tolerate `null` for
  these fields (already true given they're not marked `required` at column level).

## Status

**Spec only** — no implementation changes yet. Next step once fields are confirmed: extend
`StubReportQueryExecutor` (or the real Databricks-backed executor per `docs/spec/jdbc-connection.md`)
to emit this exact row shape, keyed by `dealerCode`.
