# Spec: Fix StubReportCallerContext authorized company code mismatch

## Problem
`POST /api/v1/reports/DEALER_LEDGER/data` is unusable: the report's config
(`GET /api/v1/reports/DEALER_LEDGER/config`) only offers `TSL` as a valid
`companyCode` SELECT option, but `StubReportCallerContext.authorizedCompanyCodes()`
returns `Set.of("TVSL")`. Every request fails either `INVALID_OPTION` (companyCode=TVSL)
or `FORBIDDEN` (companyCode=TSL) — no value satisfies both checks.

## Fix
`src/main/java/com/sapreport/dynpro/report/security/StubReportCallerContext.java`
- Change `authorizedCompanyCodes()` to `Set.of("TSL")` to match the config's
  actual option value, confirmed against the live `/config` response.

## Out of scope
- `authorizedSalesOrganisations()` (`TVSL01`) — untouched, no reported mismatch.
- No change to config parameter options or validation.

## Verification
- `mvn -q compile` succeeds.
- `curl -X POST /api/v1/reports/DEALER_LEDGER/data` with `companyCode: "TSL"`
  and a valid postingDate range returns 200 (not FORBIDDEN/VALIDATION_FAILED).
