package com.sapreport.dynpro.report.action;

import java.util.Map;
import java.util.Optional;

/**
 * Current state of one row, keyed by {@code reportCode}+{@code rowKey} —
 * what {@link ReportActionService} compares a submitted {@code rowVersion}
 * against, and updates after a successful mutation. Port only; nothing yet
 * keeps this in sync with a real data source (no {@code ReportQueryExecutor}
 * populates it from {@code /data} today) — swap in a real-store-backed
 * implementation once one exists, same as {@code ReportQueryExecutor}.
 */
public interface ReportRowStore {

    Optional<Map<String, Object>> find(String reportCode, String rowKey);

    void save(String reportCode, String rowKey, Map<String, Object> row);

    /**
     * Every row currently known for a report, keyed by {@code rowKey} —
     * needed for cross-row checks (e.g. a per-dealer invoice-number
     * uniqueness check) where the caller must exclude the row it's already
     * looking at by key. A real backing implementation would push this down
     * to an indexed/DB-level query rather than a full scan; this in-memory
     * placeholder just returns what it has.
     */
    Map<String, Map<String, Object>> findAll(String reportCode);
}
