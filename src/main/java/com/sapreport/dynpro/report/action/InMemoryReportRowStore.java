package com.sapreport.dynpro.report.action;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Placeholder {@link ReportRowStore} — in-memory, single-instance, lost on restart. Test seams seed it directly via {@link #save}. */
@Component
public class InMemoryReportRowStore implements ReportRowStore {

    private final Map<String, Map<String, Map<String, Object>>> rowsByReportCode = new ConcurrentHashMap<>();

    @Override
    public Optional<Map<String, Object>> find(String reportCode, String rowKey) {
        return Optional.ofNullable(rowsByReportCode.getOrDefault(reportCode, Map.of()).get(rowKey));
    }

    @Override
    public void save(String reportCode, String rowKey, Map<String, Object> row) {
        // Map.copyOf rejects null values, but a row's columns legitimately can be null (unset) — plain
        // LinkedHashMap + Collections.unmodifiableMap tolerates that.
        rowsByReportCode.computeIfAbsent(reportCode, code -> new ConcurrentHashMap<>())
                .put(rowKey, Collections.unmodifiableMap(new LinkedHashMap<>(row)));
    }

    @Override
    public Map<String, Map<String, Object>> findAll(String reportCode) {
        return Map.copyOf(rowsByReportCode.getOrDefault(reportCode, Map.of()));
    }
}
