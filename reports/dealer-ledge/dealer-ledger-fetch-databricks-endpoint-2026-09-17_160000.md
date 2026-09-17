# Dealer Ledger — Direct Databricks Fetch Endpoint

A new, standalone endpoint that runs the real `f_dealer_ledger` Databricks query directly, bypassing
the generic report engine (`StubReportQueryExecutor` / `/api/v1/reports/{reportCode}/data`). This is
a deliberate one-off — the existing architecture is fully generic (one `ReportController` serves every
report by `reportCode`), so this route intentionally departs from that pattern rather than extending it.

## Endpoint

| Method | Path | Purpose |
|---|---|---|
| GET | `/dealer-ledger/fetchDatabricksdata` | Runs `f_dealer_ledger` live against Databricks and returns rows |

### Query params
| Param | Type | Required | Notes |
|---|---|---|---|
| `bukrs` | string | no | company code; **default `TSL`** (confirmed real value for dealer `0000010015` via `SELECT DISTINCT bukrs ... FROM dealer_ledger_line`; `TVSL` doesn't match any data) |
| `kunnr` | string | no | dealer code; **default `0000010015`** |
| `fromDate` | date (`yyyy-MM-dd`) | no | window start; **default one month before today**. End is `fromDate + 1 month` (half-open, derived server-side, same as `dealer-ledger.sql`) |
| `limit` | int | no | caps returned rows; **default 100** |

### Response
JSON array of row objects (raw columns as returned by `f_dealer_ledger` — `credit_control_area`,
`sort_grp`, `post_date`, `doc_reference_no`, `line_item`, etc.), ordered per the underlying query.

## Query

Reuses `src/main/java/com/sapreport/dynpro/report/databricks-query/dealer-ledger.sql`, with a `LIMIT`
clause appended, bound as a fourth parameter:

```sql
SELECT *
FROM sap_dynpro.bumblebee.f_dealer_ledger(
        CAST(? AS STRING),
        CAST(? AS STRING),
        CAST(? AS DATE),
        ADD_MONTHS(CAST(? AS DATE), 1)
     )
ORDER BY credit_control_area, sort_grp, post_date, doc_reference_no, line_item
LIMIT ?
```

## Connection

Same OAuth M2M JDBC connection as `DealerLedgerQueryRunner.java` / `DatabricksQueryRunner.java`
(`com.databricks:databricks-jdbc`, `databricks.*` properties resolved from env vars or the gitignored
`application-secrets.properties`) — but wired as a Spring bean/service instead of a standalone
`main()` CLI, since this needs to run inside the running application to serve HTTP requests.

## Status

Implemented: `DealerLedgerDatabricksController` + `DealerLedgerDatabricksService` under
`com.sapreport.dynpro.report.dealerledger`.
