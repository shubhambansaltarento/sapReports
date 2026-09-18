# Spec: Rename DealerLedgerDatabricksService.fetch to fetchDealerLedgerFromBricks

## Goal
Rename the method `fetch(String bukrs, String kunnr, LocalDate fromDate, int limit)` in
`DealerLedgerDatabricksService` to `fetchDealerLedgerFromBricks` for a more descriptive,
self-documenting name (distinguishes it from other "fetch" methods across report services,
e.g. the Databricks query runners).

## Scope
- `src/main/java/com/sapreport/dynpro/report/dealerledger/DealerLedgerDatabricksService.java`
  - Rename method `fetch` -> `fetchDealerLedgerFromBricks`. Signature, body, and javadoc
    unchanged.
- `src/main/java/com/sapreport/dynpro/report/dealerledger/DealerLedgerDatabricksController.java`
  - Update the single call site (line 48) to use the new method name.

## Out of scope
- No behavior change.
- No changes to `DealerLedgerQueryRunner` (databricks package) or other services.
- No changes to REST endpoint path/contract.

## Verification
- `mvn -q compile` succeeds.
- Grep confirms no remaining references to the old `fetch(` call on
  `dealerLedgerDatabricksService`.
