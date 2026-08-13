# Dynpro — Security Configuration Specification

Status: **DRAFT — spec-first, no implementation yet.** Companion to `SPEC.md`.

This document exists because `dynpro` is a standalone backend whose only caller is a browser-based
External UI embedded inside TVS DMS — i.e. every request arrives cross-origin, from JavaScript, and
carries a business-critical token. That combination (browser caller + third-party embedding + token
handling + two outbound calls to external services) is where most of the real risk sits, so it's worth
being explicit before any code is written.

---

## 1. Transport Security

- **HTTPS/TLS only** in every environment above local dev. No plaintext HTTP listener in
  staging/prod.
- **HSTS** (`Strict-Transport-Security`) enabled at the edge/gateway in production.
- Outbound calls to the Token Verification Service and Dealer Details Service must also be TLS,
  with certificate validation enforced (no `TrustAllX509TrustManager`-style bypasses, even in dev
  configs that might accidentally ship).

---

## 2. Authentication & Authorization on Our Endpoints — DECIDED: JWT bearer, issued by `dynpro`

Two distinct concerns exist and must not be conflated:

1. **The business token** — the value the External UI sends us to be verified against the external
   Token Verification Service (`POST /api/v1/tokens/verification`). This is *payload*, not
   necessarily a platform credential.
2. **Platform-level access control** — whether every call to our API additionally needs to prove it's
   coming from an authorized client, independent of the business token's own validity.

**Decision**: `dynpro` verifies the business token once, and on success mints its **own** short-lived
JWT, which the External UI then presents as a Bearer token on every other call. This is *not* a
third-party IdP-issued OAuth2 JWT — `dynpro` is both issuer and validator. Full design, claim shape,
signing algorithm choice, and TTL are in `jwt-auth.md`. This uses the
`spring-boot-starter-security-oauth2-resource-server` dependency already in `pom.xml` for the
validation side, just pointed at our own issuer rather than an external one.

Why this over the alternatives previously on the table:
- vs. **re-verifying the business token on every call**: avoids a hard external dependency and added
  latency on every single request; the external service is only hit once per token lifetime.
- vs. **static API key/shared secret**: a static long-lived secret shipped to a browser-delivered
  frontend is itself a real exposure; a short-lived, per-session JWT minted after a real verification
  step is materially safer.

**CSRF protection is disabled** for this API: it is a stateless, token-based JSON API with no
server-side session/cookie auth, so CSRF (which protects cookie-based sessions) does not apply. This
is an explicit, documented `SecurityConfig` choice, not an oversight.

---

## 3. CORS

- Fixed **allow-list of the External UI's origin(s)**, configured per environment (dev/stage/prod) —
  no wildcard (`*`) origin, ever, since credentials/tokens are in play.
- Allowed methods: `GET, POST` (extend only if a real need arises).
- Allowed headers: `Authorization` (if/when platform auth lands), `Content-Type`, `X-Correlation-Id`.
- `Access-Control-Allow-Credentials` only if cookies end up in use — not expected given the
  token-in-payload model; keep `false` unless that changes.
- Origins must be externalized config (`application-{env}.properties` / env vars), not hardcoded, so
  DMS environment changes don't require a rebuild.

---

## 4. Secrets & Configuration

- Credentials/API keys for the external Token Verification Service and Dealer Details Service must be
  externalized (environment variables, a config server, or a secrets manager/vault) — never
  committed to `application.properties` or source control.
- No secret values in logs, error messages, or exception traces returned to the client.
- Rotate external-service credentials on a defined schedule once those integrations exist (cadence:
  TBD, follow whatever the external service provider mandates).

---

## 5. Input Validation

- Every request DTO validated via `spring-boot-starter-validation` (Bean Validation annotations);
  reject unknown/extra fields (`@JsonIgnoreProperties(ignoreUnknown = false)` or equivalent Jackson
  config) so silently-ignored typos don't become a debugging trap.
- The token value itself: bound by a reasonable max length and character-set check before it's ever
  forwarded to the external verification service, to avoid trivially abusing that call with garbage
  payloads.
- `detailType` / `searchType` discriminators (§4.3/§4.4 of `SPEC.md`) validated against the known enum
  — reject unknown values with `400` rather than silently passing them through to a downstream
  lookup.
- If any detail/search parameter ever ends up in a raw database query (via `spring-boot-starter-data-jpa`),
  it must go through parameterized queries / JPA criteria — never string-concatenated SQL/JPQL.

---

## 6. Token Handling Hygiene

- The raw business token is **never logged**, in full or truncated-but-recognizable form. If a token
  needs to appear in logs for correlation, log a one-way hash of it, not the value.
- The token verification response is **not cached** (see `SPEC.md` §4.1) — every call re-verifies, so
  a revoked/expired token can't keep passing due to stale cache state.
- The token is transmitted only in the request body (or an `Authorization` header if the
  bearer-token model is adopted) — never as a URL query parameter, since query strings end up in
  server access logs and browser history.

---

## 7. Rate Limiting & Abuse Protection — OPEN

- No rate-limiting dependency currently exists in `pom.xml`. **Needed, especially on
  `POST /api/v1/tokens/verification`**, since it's the one endpoint that fans out to an external
  service per call and is the most attractive target for brute-force/abuse.
- Proposed default (pending real requirements): per-origin/per-client token bucket, e.g. 30
  requests/minute on the verification endpoint, adjustable via config. Candidate implementation:
  `bucket4j` or a gateway-level limiter if one sits in front of this service — **decision needed**
  on where this responsibility lives (this service vs. an API gateway already in the TVS DMS
  ecosystem).

---

## 8. Resilience for External Calls — OPEN

Both the Token Verification Service and Dealer Details Service are external dependencies whose
contracts aren't finalized (`SPEC.md` §8.5/§8.7). Until real SLAs exist, propose conservative
defaults so the design isn't blocked:

| Setting | Proposed default | Applies to |
|---|---|---|
| Connect/read timeout | 3 seconds | Both external calls |
| Retry | 1 retry, idempotent GETs only (do not blindly retry token verification if the failure mode is ambiguous) | Dealer Details primarily; token verification retries need care — a retried "verify" call must not be interpretable by the external service as a second, distinct use of the token if it has one-time-use semantics (unconfirmed) |
| Circuit breaker | Open after 5 consecutive failures, half-open probe after 30s | Both |
| Failure surfaced to FE | Distinct error code (`TOKEN_VERIFICATION_UNAVAILABLE` / `DEALER_DETAILS_UNAVAILABLE`), not conflated with a "not found"/"invalid" business result | Both |

No resilience library (e.g. Resilience4j) is currently in `pom.xml` — **decision needed** on adding
one vs. hand-rolling timeout/retry with `RestClient`.

---

## 9. Actuator & Operational Endpoints

- `spring-boot-starter-actuator` is already a dependency. Only `/actuator/health` and
  `/actuator/info` should be exposed publicly (and `health` should not leak downstream-dependency
  detail to unauthenticated callers — use `management.endpoint.health.show-details=when-authorized`
  or equivalent).
- All other actuator endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/httptrace`, etc.)
  restricted to an internal network/management port, or disabled entirely in production.

---

## 10. Logging & Audit

- Every token verification attempt logged: outcome (success/failure/unavailable), correlation id,
  and dealer code/user id *if known from the claims* — never the raw token.
- Dealer details and detail/search-parameter payloads may contain business-sensitive data (GSTIN,
  PAN, contact info) — avoid dumping full response bodies at `INFO` level; if needed for debugging,
  gate behind `DEBUG` and ensure log aggregation access is restricted.
- Correlation id (`X-Correlation-Id`, `SPEC.md` §5) attached to every log line for a request so a
  DMS-reported issue can be traced end-to-end.

---

## 11. Response Hygiene

- Standard error envelope (`SPEC.md` §5) never includes raw exception messages, stack traces, or the
  external service's raw error body — map to our own `code`/`message` pairs.
- `additionalProperties: true` on pass-through DTOs (e.g. dealer details, §4.2 of `SPEC.md`) is an
  explicit, reviewed decision, not a default — any field the upstream service adds should be
  reviewed before it silently starts flowing to the FE, in case it's sensitive.

---

## 12. Summary of Open Security Decisions

| # | Item | Section | Status |
|---|---|---|---|
| 1 | Platform auth model on our endpoints | §2 | **Decided** — JWT bearer issued by `dynpro`; remaining details (algorithm, TTL, refresh) tracked in `jwt-auth.md` §7 |
| 2 | CORS allow-list values per environment | §3 | Needed (actual origin URLs) |
| 3 | Secrets storage mechanism (vault/env/config server) | §4 | Needed |
| 4 | Rate limiting ownership (this service vs. gateway) and limits | §7 | Needed |
| 5 | Resilience library choice + real timeout/retry values | §8 | Needed |
| 6 | Actuator exposure policy per environment | §9 | Needs confirmation of deployment topology |
| 7 | Report-level authorization details (which roles per report, DB topology, config storage) | — | See `jwt-auth.md`, `jdbc-connection.md`, `reports-overview.md` |
