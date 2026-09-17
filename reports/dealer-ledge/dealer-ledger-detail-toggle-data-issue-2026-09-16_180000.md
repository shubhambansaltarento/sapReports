# Dealer Ledger — "No data" when detail toggles are true

Date: 2026-09-16 18:00:00 IST
Status: **SPEC / INVESTIGATION ONLY** — no code changes made for this item.

## Report

User-reported: when the new detail toggle parameters (`withOeDetails`, `withSpDetails`,
`withAcDetails`, `withEvDetails`, `withAcwshDetails` — see
`dealer-ledger-detail-toggles-2026-09-16_172200.md`) are sent as `true`, "data is not coming".
Ask: make these parameters optional, and when not present, return the whole data.

## Investigation so far

Direct API test, `withOeDetails: true` only, all other params normal:

```json
{
  "parameters": {
    "dealerCode": "10015",
    "companyCode": "TVSL",
    "postingDate": { "from": "2026-08-16", "to": "2026-09-16" },
    "withOeDetails": true
  },
  "configVersion": "2026.09.3"
}
```

Result: `HTTP 200`, all 41 rows returned, every base column (`dealerCode`, `docType`, `docDate`,
`debit`, `credit`, etc.) populated normally. Only the newly-added `oeRefNo` column is `null` on
every row — expected, since it's a placeholder column with no real backing data yet (per the
detail-toggles spec's open item #1: real column names/values per detail type are unconfirmed).

So, at the API level:

- All 5 toggle params are already `required: false` — omitting them is safe and doesn't error.
- Setting any toggle to `true` does not filter, hide, or break the base row data — it only adds
  one extra (currently always-`null`) column to `effectiveColumns` and each row.

This means the literal ask — "make these optional, return whole data if not present" — is
**already true today** for the base ledger data. The `false` case (all present already) and
current `true` case (still returns full data, just with a null extra column) both work as
described in testing above.

## Open question — what's actually failing?

The reported symptom ("data is not coming" when true) doesn't reproduce via direct API testing.
Two candidate explanations, unconfirmed:

1. **Frontend rendering issue**: the Angular grid may require every *visible* column (including
   the new `oeRefNo`/`spRefNo`/etc.) to be non-null before it renders a row — so an all-`null`
   detail column could make rows appear to vanish, even though the API response has full data.
2. **A different request actually failing**: the real request from the UI (with real
   authentication/session context, real parameter combination) may differ from what's been
   tested here and hit a genuine error not yet reproduced.

## Next step

Need the **actual failing request/response** from the browser network tab (not a hand-constructed
curl) to confirm which of the two applies, or to find a real backend bug if neither does. No
implementation changes proposed until that's available.

## Status

**Spec / investigation note only.** No files changed.
