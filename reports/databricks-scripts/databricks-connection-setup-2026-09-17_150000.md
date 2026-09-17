# Databricks Scripts

Utility script(s) for running ad-hoc SQL queries directly against the Databricks SQL Warehouse, for exploration/debugging outside the report pipelines. See [databricks-query-catalog-2026-09-17_150500.md](databricks-query-catalog-2026-09-17_150500.md) in this folder for the per-report parameterized query catalog (`warranty-cost.sql`, `dealer-ledger.sql`, ...) and their typed-arg runners.

## Code location
Implementation lives under `src/main/java/com/sapreport/dynpro/report/databricks/` (not in this reports/ spec folder):
- `run_query.py` — CLI script that connects to a Databricks SQL Warehouse using OAuth M2M (service principal client_id/client_secret) and runs a query passed as an argument. Kept for reference, but Python isn't runnable in this environment.
- `DatabricksQueryRunner.java` — Java CLI equivalent, since Python isn't available here. Uses the `com.databricks:databricks-jdbc` driver (Maven Central) with the same OAuth M2M (client_credentials) flow, reading the same four `DATABRICKS_*` environment variables and running a query passed as `args[0]` (default `SELECT 1 AS test_col`). Run via `mvn -q compile exec:java` or directly through an IDE run configuration once compiled; no Spring context involved, plain `main()`.

No `.env` / `.env.example` files are kept in the repo. Credentials are supplied purely via process environment variables — nothing to gitignore because nothing is ever written to disk in the project tree.

## Auth
OAuth machine-to-machine (M2M) via `databricks-sdk`'s `Config`, using service principal credentials read directly from the process environment: `DATABRICKS_SERVER_HOSTNAME`, `DATABRICKS_HTTP_PATH`, `DATABRICKS_CLIENT_ID`, `DATABRICKS_CLIENT_SECRET`. No secrets in source, no secrets file.

The same four variables back the Spring Boot side. `application.properties` declares the `databricks.*` keys and optionally imports `application-secrets.properties` (`src/main/resources/application-secrets.properties`, gitignored) via `spring.config.import=optional:classpath:application-secrets.properties`. Put real `databricks.server-hostname` / `databricks.http-path` / `databricks.client-id` / `databricks.client-secret` values in that local file for local dev; it's never committed. In deployed environments, set the equivalent `DATABRICKS_*` OS environment variables instead (Spring's relaxed binding maps `DATABRICKS_CLIENT_ID` to `databricks.client-id` automatically) — either source works, and neither goes in git.

## Usage
```
pip install databricks-sql-connector databricks-sdk
export DATABRICKS_SERVER_HOSTNAME=...
export DATABRICKS_HTTP_PATH=...
export DATABRICKS_CLIENT_ID=...
export DATABRICKS_CLIENT_SECRET=...
cd src/main/java/com/sapreport/dynpro/report/databricks
python run_query.py "SELECT 1 AS test_col"
```
