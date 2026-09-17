# Repair — CORS not configured for Angular dev client

Date: 2026-09-16 12:49:31 IST

## Problem

`SecurityConfig` permitted all requests (`anyRequest().permitAll()`) but never registered a
`CorsConfigurationSource`. With no CORS configuration, the browser blocks cross-origin GET/POST
calls from the Angular dev server at `http://localhost:4200` to this API on `http://localhost:8080`
— the preflight `OPTIONS` request gets no `Access-Control-Allow-Origin` header, so the browser never
lets the actual request through, even though the endpoint itself works fine when called directly
(e.g. via curl or Postman).

Separately, the server also failed to start once because port 8080 was already occupied by a
leftover `java.exe` process from a previous run; that process was killed before restarting.

## Fix

`src/main/java/com/sapreport/dynpro/config/SecurityConfig.java`:

- Added a `CorsConfigurationSource` bean allowing origin `http://localhost:4200`, methods
  `GET, POST, PUT, PATCH, DELETE, OPTIONS`, all headers, and credentials.
- Wired it into the filter chain via `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`.

This is scoped to local Angular dev (`localhost:4200`) only. When a real deployed External UI origin
is known, add it to the allowed-origins list (see `docs/spec/SPEC.md` §6 — CORS is called out there as
an explicit per-environment allow-list, no wildcard).

## Verification

- `./mvnw.cmd compile` — builds clean.
- Server started successfully on port 8080 (`Started DynproApplication in 1.777 seconds`).
- `curl -i -X OPTIONS http://localhost:8080/api/v1/reports/test/config -H "Origin: http://localhost:4200" -H "Access-Control-Request-Method: GET"`
  returns `200` with `Access-Control-Allow-Origin: http://localhost:4200`.

## Follow-up

- Add production/staging External UI origins to the allow-list once known (currently hardcoded to
  `localhost:4200` only, per the TEMPORARY note already on `SecurityConfig`).
