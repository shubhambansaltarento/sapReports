# Dealer Ledger / Warranty Reports — Company Code Fix (TVSL → TSL)

## Problem
`companyCode` parameter across the report configs defaulted to `TVSL` (label "TVS Lucas"), but that
value doesn't match any real data. Confirmed via direct Databricks query against
`sap_dynpro.bumblebee.dealer_ledger_line`:

```sql
SELECT DISTINCT bukrs, kunnr, MIN(post_date), MAX(post_date)
FROM sap_dynpro.bumblebee.dealer_ledger_line
WHERE kunnr = '0000010015'
GROUP BY bukrs, kunnr
```

Real `bukrs` values present: `TSL`, `7770`, `9990` — no `TVSL`. Confirmed live: the standalone
`/dealer-ledger/fetchDatabricksdata` endpoint returns 0 rows with `bukrs=TVSL`, but real data (100
rows for dealer `0000010015`) with `bukrs=TSL`.

## Fix
`companyCode` param `defaultValue` and its `options` list changed from `TVSL` ("TVS Lucas") to `TSL`
("TVS Lucas") in:
- `src/main/resources/reports/dealer-ledger.json`
- `src/main/resources/reports/warranty-cost.json`
- `src/main/resources/reports/warranty-reconcillation.json`

(All three share the same wrong default — same underlying company code, so all three needed the same fix.)

## Status
Implemented.
