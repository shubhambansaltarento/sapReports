# Databricks Query Catalog

A `.sql` file per report, holding the exact Databricks query that backs it, with named parameters instead of hardcoded/positional values. This is the source of truth for what each report actually runs against `sap_dynpro.bumblebee.*` once the stub executors (`StubReportQueryExecutor`) are replaced with real ones — see [databricks-connection-setup-2026-09-17_150000.md](databricks-connection-setup-2026-09-17_150000.md) in this folder for the connection/auth side (`DatabricksQueryRunner`, `run_query.py`).

## Code location
The `.sql` files live under `src/main/java/com/sapreport/dynpro/report/databricks-query/` (not this docs folder).

## Convention

- One file per report, named `<report-code-kebab-case>.sql` (e.g. `warranty-cost.sql` for `WARRANTY_COST`).
- Parameters are named, `:likeThis`, matching the corresponding key in that report's `ReportQueryRequest.parameters()` map (camelCase, same names the stub executors already use — `dealerCode`, `claimDate`, `reconciliationDate`, etc.).
- A `-- params:` comment header lists each parameter with its type, so a query file is self-describing without needing to open the Java executor.
- Values are never inlined; a future JDBC-backed executor binds these named parameters positionally (or via `NamedParameterJdbcTemplate`-style substitution) instead of string-concatenating input.
- Each query has a matching `<ReportName>QueryRunner.java` CLI, but that class lives in `src/main/java/com/sapreport/dynpro/report/databricks/` (not the `databricks-query/` folder holding the `.sql` files) since `databricks-query` isn't a valid Java package segment — its `package` declaration must match its actual path.

## Queries

### warranty-cost.sql
Backs `WARRANTY_COST`. Wraps the `api_warranty_cost` table function, parameterized by dealer code and a claim-date range.
- Params: `dealerCode` (string), `fromDate` (date), `toDate` (date)
- Runner: `WarrantyCostQueryRunner.java` — usage: `java WarrantyCostQueryRunner <dealerCode> <fromDate> <toDate>`

### dealer-ledger.sql
Backs `DEALER_LEDGER`. Wraps `f_dealer_ledger`, parameterized by company code (`bukrs`) and dealer code (`kunnr`), over a one-month half-open window starting at `from_date` (the end bound is derived server-side via `ADD_MONTHS(from_date, 1)`, not a separate input).
- Params: `bukrs` (string), `kunnr` (string), `from_date` (date)
- Runner: `DealerLedgerQueryRunner.java` — usage: `java DealerLedgerQueryRunner <bukrs> <kunnr> <fromDate>`
