# Dynpro — JDBC Connection Design

Status: **DRAFT — design only.** Companion to `reports-overview.md`: the 8+ reports all need to pull
data from a database, so this fixes how the app talks to it before any report-specific work starts.

---

## 1. Why this needs its own design

`spring-boot-starter-data-jpa` is already in `pom.xml`, but reports are typically **read-heavy, dynamic,
parameter-driven queries** (arbitrary date ranges, optional filters, joins across many tables) rather
than the clean create/read/update/delete-a-single-entity pattern JPA is built for. Deciding the
connection and query-execution strategy up front avoids each of the 8+ report components inventing its
own approach.

---

## 2. How many databases? — OPEN QUESTION

Not yet known:

- Is there a **single** source database all 8+ reports query (e.g. a reporting replica of the DMS/SAP
  system)?
- Or do different reports pull from **different** source systems (e.g. some from a DMS database,
  others directly from SAP tables)?
- Does `dynpro` need a **second, small database of its own** — e.g. if report configs
  (`reports-overview.md` §3) end up living in a table rather than a static file, or if we want an audit
  log of report executions?

The design below assumes **one primary reporting connection**, with a second optional connection called
out separately — adjust once the real topology is confirmed.

---

## 3. Proposed structure

### 3.1 Primary DataSource — "Reporting" connection

- A single Spring `DataSource` bean pointed at the source-of-truth database the reports query.
- Configuration externalized (`security.md` §4): URL, username, password, driver class name, and pool
  settings all come from environment/config, never hardcoded — `spring.datasource.reporting.*`-style
  properties, distinct per environment (dev/stage/prod).
- **Read-only enforced at two levels**, not just convention:
  1. Application level: queries run inside `@Transactional(readOnly = true)`.
  2. Database level (stronger guarantee): the DB user/account `dynpro` connects with should only hold
     `SELECT` grants on the relevant tables/views, so even a bug in our code cannot write to the source
     system. **Recommend this be a hard requirement**, not just an app-level convention.
- **Connection pooling** via HikariCP (Spring Boot's default pool). Reporting queries can run longer
  and heavier than typical CRUD, so pool sizing needs deliberate attention rather than defaults:
  - Max pool size sized to expected concurrent report requests (needs real numbers — **open
    question**, depends on expected DMS user concurrency).
  - Connection/idle timeouts set so a stuck query surfaces as a clear timeout rather than silently
    exhausting the pool for everyone else.
  - Leak-detection threshold enabled, so a connection that's checked out and never returned (a bug)
    is logged loudly instead of quietly starving the pool over time.

### 3.2 Secondary "app config/audit" DataSource — optional, TBD

- Only needed if report configs (`reports-overview.md` §3) are stored in a database table instead of
  static files, or if we want to persist a log of report executions (who ran what report, with which
  parameters, when — useful for both audit and troubleshooting).
- If needed, this can reasonably be JPA-backed (`spring-boot-starter-data-jpa`) since it *is*
  naturally entity-shaped data (a `ReportConfig` entity, an `ReportExecutionLog` entity) — unlike the
  reporting queries themselves.
- **Open question**: do you want report configs in a database (editable without a redeploy) or as
  static files bundled with the app (simpler, but requires a deploy to change)? Either is workable;
  this affects whether this second DataSource is needed at all.

---

## 4. Query execution style

- **Recommend `NamedParameterJdbcTemplate`** (Spring's JDBC helper) for the actual report queries, not
  JPA entities — reports are read-heavy, have dynamic WHERE clauses depending on which optional search
  parameters were supplied, and often correspond to existing hand-tuned SQL or stored procedures on the
  source-system side rather than a clean object model dynpro would own.
- JPA stays reserved for anything that genuinely *is* one of our own entities (e.g. the optional
  config/audit tables in §3.2).
- **Every parameter bound via named/positional JDBC parameters — never string-concatenated SQL.** This
  is the concrete mechanism behind `security.md` §5's SQL-injection rule; it's non-negotiable given
  these queries are driven by caller-supplied search parameters.

---

## 5. How this wires into a report component

Each report (`reports-overview.md` §5, the `ReportComponent` design) references, in its config, either:

- a named SQL resource (e.g. `dealer-ledger.sql`) run via `NamedParameterJdbcTemplate`, or
- a stored procedure name, invoked via `SimpleJdbcCall` or an explicit `CallableStatement`.

The component takes the caller's validated search parameters (already checked against the report's
declared filter definitions, `reports-overview.md` §3), binds them to the query/procedure call, and maps
the resulting rows into that report's contract (`reports-overview.md` §4).

---

## 6. Open Questions

1. Number and identity of actual source database(s)/system(s) — one shared reporting DB, or several
   per report?
2. Whether the DMS/SAP side already has an established convention (stored procedures vs. raw SQL) we
   should follow rather than introduce our own.
3. Whether report configs live in a database table or as static files (decides whether §3.2's second
   DataSource is needed at all).
4. Expected concurrency/data volume per report — needed to size the connection pool and decide whether
   pagination is required on report results (`SPEC.md` §5 currently has no pagination convention
   defined for report-shaped responses).
5. Whether a DB user with SELECT-only grants is actually obtainable for the source system, or whether
   read-only enforcement will have to rely on the application layer alone.
