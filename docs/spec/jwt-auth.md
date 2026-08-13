# Dynpro — JWT Authentication & Authorization Design

Status: **DRAFT — design only.** Per your instruction, actual Spring Security wiring is deferred; this
document fixes the shape now so nothing downstream (report access rules, controller design) is guessing.
This also **resolves** `security.md` §2 / `SPEC.md` §8.6, which were previously "decision pending":
the platform auth model **is** JWT bearer, issued by `dynpro` itself.

---

## 1. Why a JWT layer on top of token verification

`POST /api/v1/tokens/verification` (`SPEC.md` §4.1) already checks the DMS-issued business token against
the external Token Verification Service. Re-running that external check on *every* call — every report
search, every detail fetch — would add an external round-trip and a hard dependency to endpoints that
don't need one.

**Design**: `dynpro` verifies the business token once, and on success mints its **own** short-lived JWT
("access token"). The External UI then sends that JWT as a Bearer token on every subsequent call
(`/dealers/{dealerCode}`, `/details`, `/search-parameters`). Those endpoints validate the JWT locally
(signature + expiry check) — cheap, no external call, no dependency on the verification service being
up for every single click in the UI.

**Analogy** (since this is your first Java project): the business token is like a passport, checked once
at immigration (the external verification service). Our JWT is the boarding pass issued after that check
— gate staff (our other endpoints) can verify a boarding pass locally and quickly; they don't re-run your
passport through immigration at every gate.

---

## 2. Token contents (claims)

| Claim | Meaning | Source |
|---|---|---|
| `iss` | `"dynpro"` | Fixed |
| `sub` | User id | From the external verification service's claims (`SPEC.md` §8.5 — placeholder until real contract lands) |
| `dealerCode` | Dealer code the caller is acting as | Same source |
| `roles` | Array of role/permission strings, used for report-level authorization | Same source — **exact vocabulary TBD**, depends on the external service and on each report's required role(s), which will arrive with the report-wise specs |
| `iat` / `exp` | Issued-at / expiry | Set by `dynpro` at mint time |
| `jti` | Unique token id | Set by `dynpro`; reserved for future revocation/blacklist support if ever needed |

---

## 3. Signing algorithm — recommendation: HS256 to start

- **HS256** (single shared secret): `dynpro` is both the sole issuer and sole validator of this token —
  no other service needs to check it independently yet — so a shared secret is simplest to build and
  operate. Secret lives outside source code per `security.md` §4 (env var / vault).
- **RS256** (public/private keypair): worth moving to later if another service ever needs to validate
  our tokens independently, or if key rotation via a JWKS endpoint becomes a requirement. Spring's
  `oauth2-resource-server` (already in `pom.xml`) supports either.
- **Open decision**: default to HS256 unless you already know a second validator is coming.

---

## 4. Token lifetime & renewal

- **Proposed TTL: 15–30 minutes** for the access token — short enough that a leaked token has a small
  blast radius, long enough to cover normal use of a search/detail screen without constant re-issuance.
  **Open question**: what's a realistic session length for someone working in these DMS-embedded report
  screens? That should drive the real number.
- **No refresh token in v1.** When the JWT expires, the External UI simply calls
  `POST /api/v1/tokens/verification` again with the original business token to get a new one. Simpler
  than building refresh-token rotation. This assumes the business token itself stays valid/available to
  the External UI for the life of the DMS session — **flagging as an open question**, since if the
  business token is itself single-use or very short-lived, this renewal approach won't work and a real
  refresh-token flow would be needed instead.

---

## 5. Enforcement shape (conceptual — no code yet)

- `POST /api/v1/tokens/verification` — the **one** endpoint that does **not** require our JWT (it's where
  the JWT is issued — nothing to validate yet). Still protected by rate limiting (`security.md` §7).
  `openapi.yaml` marks this operation with an explicit empty `security: []` for this reason, not because
  it was overlooked.
- Every other endpoint (`/dealers/{dealerCode}`, `/details`, `/search-parameters`) — protected by Spring
  Security's OAuth2 Resource Server support (`spring-boot-starter-security-oauth2-resource-server`,
  already in `pom.xml`), configured to validate `dynpro`'s own locally-issued JWT rather than a
  third-party IdP's tokens.
- **Authorization**: once a report's required role(s) are known (arriving with its report-wise spec,
  see `reports-overview.md` §6), a method-level check (e.g. `@PreAuthorize`) on `/details` and
  `/search-parameters` verifies the caller's `roles` claim includes whatever that specific report
  requires, before dispatching to its `ReportComponent`.

---

## 6. Failure modes

| Situation | Response |
|---|---|
| Missing/malformed JWT on a protected endpoint | `401`, code `AUTH_TOKEN_INVALID` |
| Expired JWT | `401`, code `AUTH_TOKEN_EXPIRED` (distinct from invalid, so the FE knows to silently re-verify rather than treat it as a hard failure) |
| Valid JWT, but caller's `roles` don't cover the requested report | `403`, code `REPORT_ACCESS_DENIED` |

---

## 7. Open Questions

1. Real claims vocabulary (`roles` values, exact `sub`/`dealerCode` field presence) — depends on the
   external verification service's real contract (`SPEC.md` §8.5).
2. HS256 vs RS256 — defaulting to HS256 unless a second validator is anticipated.
3. Real access-token TTL — defaulting to 15–30 min pending confirmation.
4. Whether the "re-verify to renew" approach is viable, or a real refresh-token flow is needed — depends
   on the business token's own lifetime/reusability, which isn't known yet.
5. Per-report required role(s) — will arrive with each report's report-wise spec (`reports-overview.md`).
