package com.sapreport.dynpro.report.query;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholder {@link ReportQueryExecutor} returning canned in-memory data
 * for DEALER_LEDGER so the API is exercisable end-to-end (e.g. via Swagger)
 * before the real Databricks-backed implementation exists (jdbc-connection.md).
 */
@Component
public class StubReportQueryExecutor implements ReportQueryExecutor {

    @Override
    public ReportQueryResult execute(ReportQueryRequest request) {
        if (!"DEALER_LEDGER".equals(request.reportCode())) {
            return new ReportQueryResult(List.of(), Map.of(), 0, Instant.now(), 0);
        }

        List<Map<String, Object>> allRows = List.of(
                fullRow("2026-04-02", "12500.00", "0.00", "12500.00", "CBL-9921"),
                fullRow("2026-04-15", "0.00", "5000.00", "7500.00", "CBL-9930"),
                fullRow("2026-05-01", "3200.50", "0.00", "10700.50", "CBL-9944")
        );

        List<Map<String, Object>> projected = allRows.stream()
                .map(row -> project(row, request.effectiveColumns()))
                .toList();

        int fromIndex = Math.min((Math.max(request.page(), 1) - 1) * request.pageSize(), projected.size());
        int toIndex = Math.min(fromIndex + request.pageSize(), projected.size());
        List<Map<String, Object>> page = projected.subList(fromIndex, toIndex);

        Map<String, Object> totals = Map.of(
                "debit", new BigDecimal("15700.50"),
                "credit", new BigDecimal("5000.00")
        );

        return new ReportQueryResult(page, totals, projected.size(), Instant.now(), 5);
    }

    private Map<String, Object> fullRow(String postingDate, String debit, String credit, String runningBalance,
                                         String cblRefNo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("postingDate", postingDate);
        row.put("debit", new BigDecimal(debit));
        row.put("credit", new BigDecimal(credit));
        row.put("runningBalance", new BigDecimal(runningBalance));
        row.put("cblRefNo", cblRefNo);
        return row;
    }

    private Map<String, Object> project(Map<String, Object> row, List<String> effectiveColumns) {
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String column : effectiveColumns) {
            if (row.containsKey(column)) {
                projected.put(column, row.get(column));
            }
        }
        return projected;
    }
}
