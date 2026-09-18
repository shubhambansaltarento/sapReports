# Parts Packing List — Databricks Query Spec

## Query
```sql
SELECT *
FROM sap_dynpro.bumblebee.fn_parts_packing_list(
        CAST(:dealerCode AS STRING),
        CAST(:fromDate   AS DATE),
        CAST(:toDate     AS DATE)
     )
```

## Params
| Param | Type | Notes |
|---|---|---|
| `dealerCode` | string | e.g. `'0000010015'` |
| `fromDate` | date | window start, e.g. `'2026-08-01'` |
| `toDate` | date | window end, e.g. `'2026-09-01'` |

Unlike `dealer-ledger.sql` (where the end date is derived server-side via `ADD_MONTHS`), both bounds
are explicit inputs here — matching `warranty-cost.sql`'s two-sided range style, not `dealer-ledger`'s
one-sided-plus-derived-end style.

## Source of params
All three are supplied by the caller — `dealerCode`, `fromDate`, `toDate` come from the API request
payload (not defaulted, not derived), same as `WARRANTY_COST`'s `dealerCode`/`claimDate` range params
in the generic report engine.

## Code location (per repo convention)
- `.sql` file: `src/main/java/com/sapreport/dynpro/report/databricks-query/parts-packing-list.sql`
- Standalone Java runner (bound via `PreparedStatement`, mirrors `WarrantyCostQueryRunner`/
  `DealerLedgerQueryRunner`): `PartsPackingListQueryRunner.java`, in
  `src/main/java/com/sapreport/dynpro/report/databricks/` (not the `databricks-query/` folder — same
  package/path-must-match reasoning as the other runners).

## Status
Implemented:
- `parts-packing-list.sql`, `PartsPackingListQueryRunner.java` (CLI, same shape as `WarrantyCostQueryRunner`/`DealerLedgerQueryRunner`).
- `POST /parts-packing-list/fetchDatabricksdata` — `PartsPackingListDatabricksController`/`Service` under `com.sapreport.dynpro.report.partspackinglist`, taking `{dealerCode, fromDate, toDate}` as a JSON request body (payload-bound, unlike `/dealer-ledger/fetchDatabricksdata` which uses query params).
- `GET /parts-packing-list/config` — basic parameter metadata (`dealerCode`/`fromDate`/`toDate`, all required), not the full generic `ReportMetadata` shape used by `/api/v1/reports/{reportCode}/config`.
