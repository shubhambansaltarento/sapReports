# Dynpro — Dealer Detail Service — Functional & API Specification

Status: **DRAFT — spec-first, no implementation yet.** Sections marked `OPEN QUESTION` block finalization
of that section only; the rest of the document can proceed in parallel.

---

## 1. Purpose & Scope

`dynpro` (`com.sapreport.dynpro`) is a **standalone Spring Boot REST API service**. It is not itself
embedded in TVS DMS. Instead:

- TVS DMS embeds a **separate external UI application** (not part of this repository).
- That external UI calls this service's REST APIs **directly from the browser**.
- This service, in turn, calls out to external systems (a token verification service and a dealer
  details source) and returns their results — sometimes close to verbatim — to the external UI.

This document specifies:

1. Token verification (proxy to an external verification service).
2. Dealer details retrieval.
3. A generic "Get Detail" API design, reused across 8 UI screens with different parameters.
4. Eight "Search Parameters" APIs that back the search/filter UI for those screens.

---

## 2. System Context

```
 ┌─────────────┐      iframe/embed       ┌───────────────────┐
 │   TVS DMS    │ ───────────────────────▶│   External UI      │
 └─────────────┘                          │ (separate app,     │
                                           │  not in this repo) │
                                           └─────────┬──────────┘
                                                     │ HTTPS, browser-origin
                                                     │ (CORS allow-listed)
                                                     ▼
                                           ┌───────────────────┐
                                           │   dynpro API       │  ← this service
                                           └─────────┬──────────┘
                                     ┌───────────────┼────────────────┐
                                     ▼                                ▼
                        ┌────────────────────┐          ┌──────────────────────┐
                        │ External Token       │          │ External Dealer       │
                        │ Verification Service │          │ Details Service       │
                        │ (contract TBD)        │          │ (contract TBD)        │
                        └────────────────────┘          └──────────────────────┘
```

**Actors**

| Actor | Role |
|---|---|
| TVS DMS | Host application; embeds the External UI. Not a direct caller of this API. |
| External UI | Browser app embedded in DMS; the actual caller of every endpoint in this spec. |
| dynpro API | This service. Verifies tokens, fetches dealer details, serves detail/search-parameter data. |
| External Token Verification Service | Third-party/internal system that validates a caller-supplied token. **Contract not yet available** — see §8.5. |
| External Dealer Details Service | Source of dealer master data. **Contract not yet confirmed** — see §8.7. |

---

## 3. High-Level Flow

1. TVS DMS obtains/issues a token through a mechanism outside this service's scope, and it ends up in
   the External UI's hands.
2. External UI calls `POST /api/v1/tokens/verification` with that token.
3. `dynpro` calls the external Token Verification Service via an internal adapter and returns the
   verification result **as the HTTP response body** — the External UI consumes this response directly,
   it is not just a pass/fail flag.
4. Using data obtained from the verified token (assumed: a dealer code — see §8.5), the External UI (or
   `dynpro` internally, TBD) triggers the Dealer Details flow to fetch and return dealer details.
5. External UI calls the generic **Get Detail API** (`POST /api/v1/details`) for each of its 8 detail
   screens, each supplying a different `detailType` and its own parameter set.
6. External UI calls the corresponding **Search Parameters API** (`POST /api/v1/search-parameters`) to
   populate filter/lookup UI (dropdowns, date ranges, typeaheads, etc.) for each of the 8 search screens.

---

## 4. API Groups

### 4.1 Token Verification API

- `POST /api/v1/tokens/verification`
- **Purpose**: Accept a caller-supplied token, verify it against the external Token Verification
  Service, and return the verification result to the caller as-is (shaped into our envelope).
- **Request**: `{ "token": "<opaque string>" }`
- **Response**: verification outcome + claims extracted from the token (dealer code, user id, roles,
  expiry — see §8.5 for exact fields, currently placeholder).
- **Not cached.** Token state (validity, expiry, revocation) can change between calls; every request
  re-verifies.
- **Failure modes**: external service timeout/5xx/unreachable must map to a distinct error code
  (`TOKEN_VERIFICATION_UNAVAILABLE`) rather than being conflated with "invalid token"
  (`TOKEN_INVALID`), so the FE can distinguish "try again" from "log in again."
- **Also issues our own JWT** on success — see `jwt-auth.md`. The response includes an `accessToken`
  the External UI then uses as a Bearer token on every other endpoint below, so those endpoints don't
  need to re-verify the business token on every call.
- Full detail in `openapi.yaml`.

### 4.2 Dealer Details API

- `GET /api/v1/dealers/{dealerCode}`
- **Purpose**: Return dealer master details for the dealer identified by `dealerCode`.
- **Source of `dealerCode`**: assumed to come from the verified-token claims (§8.5) or be supplied
  directly by the External UI — **OPEN QUESTION**, see §8.4.
- **Response shape**: whether this is a verbatim pass-through of the upstream Dealer Details Service or
  a curated subset for the FE contract is **OPEN**, see §8.4/§8.7. This spec defines a placeholder
  field set now (dealer code, name, address, GSTIN/PAN, type, status, region, contact info) with
  `additionalProperties: true` so upstream extra fields pass through without breaking clients.
- **Cacheable**: dealer master data changes infrequently. Recommend a short-to-medium TTL cache
  (`spring-boot-starter-cache` is already a dependency) — default proposal: 5 minutes, configurable.

### 4.3 Get Detail API (generic design, reused across 8 places)

- `POST /api/v1/details`
- **Purpose**: One technical endpoint design reused by 8 different UI detail screens, each
  distinguished by a `detailType` discriminator and its own parameter payload.
- **Request**: `{ "detailType": "<enum>", "parameters": { ...type-specific... } }`
- **Response**: `{ "detailType": "<enum>", "data": { ...type-specific... } }`
- The 8 "places" are now known to be the **report list** in `reports-overview.md` §2 (10 entries once
  the Warranty Labour Tax Invoice split is counted — see that document's count note). The `detailType`
  enum in `openapi.yaml` has been updated to those report keys, replacing the earlier
  `DETAIL_TYPE_1..8` placeholders. Per-report parameter/response shapes are still TBD — arriving as
  report-wise specs, see `reports-overview.md` §6.
- Rationale for one generic endpoint vs. 10 discrete ones: matches the requirement as stated ("an API …
  used for 8 places with different parameters") and keeps routing/auth/logging centralized — and now
  also keeps a single `ReportComponent` registry (`reports-overview.md` §5) instead of per-report
  controllers. If some reports turn out to need materially different response shapes or access rules,
  reconsider splitting — flagged for revisit once the report-wise specs are in.

### 4.4 Search Parameters APIs (8 endpoints, one generic design)

- `POST /api/v1/search-parameters`
- **Purpose**: Return the filter/lookup metadata needed to render a search screen's filter UI (field
  list, field types, dropdown options, required/optional, ranges) for one of 8 search screens.
- **Working interpretation of "reverting search parameters"**: read as *retrieving* the set of valid
  search parameters/filter options for a screen, not a literal "revert/undo" action — reinforced by the
  per-report "config file for search parameters" design in `reports-overview.md` §3, which is exactly
  this retrieval. Treating this as confirmed unless you say otherwise.
- **Request**: `{ "searchType": "<enum>", "context": { ...optional scoping params... } }`
- **Response**: `{ "searchType": "<enum>", "filters": [ { "field", "label", "type", "required",
  "options"? } ] }`
- `searchType` enum now holds the same **report keys** as `detailType` (§4.3) — the 1:1 mapping
  `§8.3` previously flagged as unconfirmed is confirmed by `reports-overview.md` §1: one report per
  key, one search-parameters config and one get-detail behavior each.

---

## 5. Common API Conventions

- Base path: `/api/v1`.
- JSON only, UTF-8, `Content-Type: application/json`.
- Standard error envelope (all non-2xx responses):
  ```json
  {
    "timestamp": "2026-08-13T10:15:30Z",
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Human-readable summary",
    "path": "/api/v1/details",
    "correlationId": "b3c1..."
  }
  ```
- `X-Correlation-Id` request header: propagated if supplied by the caller, otherwise generated and
  echoed back on the response, and attached to all log lines for that request.
- Versioning via URI prefix (`/v1`); breaking changes get a new prefix rather than mutating `/v1` in place.
- No pagination is currently specified for detail/search-parameter responses since shapes are unknown;
  revisit once §9.1/§9.2 are filled in — list-shaped responses should get `page`/`size`/`sort` query
  params and a `{ items, page, size, totalElements }` envelope if needed.

---

## 6. Security (summary — full detail in `security.md`)

- **CORS**: explicit per-environment allow-list of the External UI's origin(s); no wildcard.
- **Platform-level authentication on our endpoints**: **decided** — JWT bearer, issued by `dynpro`
  itself after successful business-token verification (not a third-party IdP token). Full design in
  `jwt-auth.md`; `security.md` §2 updated accordingly.
- **Transport**: HTTPS/TLS only, HSTS in production.
- **Secrets**: externalized via environment/config server, never committed to source; see
  `security.md` §4.
- **Input validation**: Bean Validation (`spring-boot-starter-validation`) on every request DTO;
  reject unknown fields.
- **Audit/logging**: token verification attempts logged (success/failure, correlation id, dealer code
  if known) without ever logging the raw token value.
- See `security.md` for full details, including CSRF, actuator exposure, rate limiting, and resilience
  (timeouts/retries) for the two external service calls.

---

## 7. Non-Functional Requirements

| Area | Status |
|---|---|
| Availability target | **OPEN** — no SLA specified yet. |
| External call timeout/retry policy | **OPEN** — see §8.8, defaults proposed in `security.md`. |
| Caching TTLs (dealer details, detail/search metadata) | **OPEN** — see §8.9, defaults proposed. |
| Rate limiting | **OPEN** — see §8.10, especially important on the token-verification endpoint. |
| Observability | Actuator health/metrics already in `pom.xml`; structured logs + correlation id per §5. |

---

## 8. Open Questions / Assumptions

These are called out inline above; consolidated here for tracking. Each states the assumption this
spec currently uses so work isn't blocked, and what's needed to close it out.

1. ~~**"8 APIs for reverting search parameters" — meaning.**~~ **Resolved**: retrieving search
   filter/lookup metadata, one generic endpoint reused per report (§4.4), confirmed by the per-report
   config-file design in `reports-overview.md` §3.
2. ~~**The 8 "Get Detail" places.**~~ **Mostly resolved**: they are the report list in
   `reports-overview.md` §2 (10 entries — see that doc's count note re: the stated "8"). Still
   **needed**: per-report parameters/columns/query, arriving as report-wise specs
   (`reports-overview.md` §6).
3. ~~**The 8 "Search Parameters" places.**~~ **Resolved**: 1:1 with the Get Detail places, both keyed
   by the same report-key enum (`reports-overview.md` §1).
4. **Dealer details response shape.** Pass-through of the upstream service vs. a curated FE
   contract — **undecided**. Also unclear whether `dealerCode` for `GET
   /api/v1/dealers/{dealerCode}` always comes from the verified token or can be supplied directly
   by the External UI.
5. **Token verification service contract.** Not available yet ("will come later"). This spec
   defines an internal adapter interface and a placeholder response shape (validity flag + claims:
   dealer code, user id, roles, expiry) so the rest of the design isn't blocked. **Needs the real
   contract** (sync/async, endpoint, auth, exact claim names) once available.
6. ~~**Platform-level auth on our own endpoints.**~~ **Decided**: JWT bearer, issued by `dynpro`
   after business-token verification — see `jwt-auth.md`. Remaining open items on *that* decision
   (HS256 vs RS256, TTL, refresh approach) are tracked in `jwt-auth.md` §7, not here.
7. **External Dealer Details service.** Same provider as the token verification service, or a
   different one? REST/SOAP, sync/async, auth mechanism — **unknown, needed**.
8. **Resilience settings** for both external calls: timeout values, retry counts, circuit-breaker
   thresholds. **Needed**; `security.md` proposes conservative defaults (e.g. 3s timeout, 1 retry)
   to use until real SLAs are known. Note: no resilience library (e.g. Resilience4j) is in `pom.xml`
   yet — **decision needed** on whether to add one or rely on plain `RestClient` timeouts.
9. **Cache TTLs** for dealer details and detail/search-parameter metadata. Defaults proposed
   (5 min) — **confirm or override**.
10. **Rate limiting** requirements, especially on `POST /api/v1/tokens/verification` to prevent
    brute-force/abuse. **Needed**; no rate-limiting dependency currently in `pom.xml`.
11. **Report count mismatch.** You said "8 components for 8 reports," but the named list has 9
    top-level reports, and splitting Warranty Labour Tax Invoice into its 2 sub-reports makes 10
    total. See `reports-overview.md` §2 — needs confirmation before the `detailType`/`searchType`
    enums are locked.
12. **Source database topology.** One shared reporting database for all reports, or several per
    report/system? See `jdbc-connection.md` §2.
13. **Report config storage.** Static files bundled with the app, or a database table? See
    `jdbc-connection.md` §3.2 / `reports-overview.md` §3.
14. **Report export formats.** PDF/Excel/CSV export wasn't mentioned but is common for this kind of
    report — confirm if/which formats and for which reports. See `reports-overview.md` §4.

---

## 9. Appendix — Report Keys (formerly Placeholder Tables)

Superseded by `reports-overview.md` §2, which now holds the authoritative report list, and §6, which
tracks what's still needed per report (parameters, columns, query, role). Kept here only as a pointer
so old links to "§9.1/§9.2" still resolve to the right place.

---

## 10. Related Documents

- `security.md` — security configuration in detail.
- `jwt-auth.md` — JWT authentication & authorization design (resolves §8.6).
- `jdbc-connection.md` — database connection design for the reports.
- `reports-overview.md` — common report component design; authoritative report list (resolves §8.2/§8.3).
- `openapi.yaml` — design-first OpenAPI 3.0 specification (Swagger).
