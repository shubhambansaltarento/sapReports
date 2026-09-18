# Spec: Rename Databricks fetch endpoint paths to kebab-case

## Goal
Rename the "fetch data from Databricks" REST endpoint paths to a consistent kebab-case
convention, matching the pattern `dealer-ledger/fetch-data-bricks-data`.

## Scope
- `DealerLedgerDatabricksController`
  - `GET /dealer-ledger/fetchDatabricksdata` -> `GET /dealer-ledger/fetch-data-bricks-data`
- `PartsPackingListDatabricksController`
  - `POST /parts-packing-list/fetchDatabricksdata` -> `POST /parts-packing-list/fetch-data-bricks-data`

Java method names, service methods, and Swagger `@Operation` summaries/descriptions are
untouched except where they textually reference the old path in a doc comment
(update `fetchDatabricksdata` mentions in Javadoc/description strings to the new path
for accuracy).

## Out of scope
- `/parts-packing-list/config` path (not a fetch endpoint).
- `/api/v1/reports/**` (ReportController) — not Databricks-specific, unaffected.
- No change to request/response payloads, HTTP verbs, or query/body param names.

## Verification
- `mvn -q compile` succeeds.
- `curl` both renamed endpoints and confirm `200` responses.
- Old paths (`fetchDatabricksdata`) return 404 (no back-compat aliasing requested).
