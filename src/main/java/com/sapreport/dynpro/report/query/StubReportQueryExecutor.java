package com.sapreport.dynpro.report.query;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Placeholder {@link ReportQueryExecutor} returning canned in-memory data
 * for DEALER_LEDGER and WARRANTY_COST so the API is exercisable end-to-end
 * (e.g. via Swagger) before the real Databricks-backed implementation exists
 * (jdbc-connection.md). DEALER_LEDGER row values are transcribed from dealer
 * 10015's ledger extract (reports/dealer-ledge/spec.md) so the API response
 * matches what the Angular grid is expected to render. WARRANTY_COST rows are
 * dummy data for the same stub dealer (reports/warranty-cost/
 * warranty-cost-data-api-2026-09-17_140000.md).
 */
@Component
public class StubReportQueryExecutor implements ReportQueryExecutor {

    private static final String DEALER_CODE = "10015";
    private static final String CCA = "ZTS1";
    private static final DateTimeFormatter ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public ReportQueryResult execute(ReportQueryRequest request) {
        if ("WARRANTY_COST".equals(request.reportCode())) {
            return executeWarrantyCost(request);
        }
        if (!"DEALER_LEDGER".equals(request.reportCode())) {
            return new ReportQueryResult(List.of(), Map.of(), 0, Instant.now(), 0);
        }

        List<Map<String, Object>> allRows = buildRows();

        Object dealerCode = request.parameters().get("dealerCode");
        List<Map<String, Object>> filtered = allRows.stream()
                .filter(row -> dealerCode == null || String.valueOf(dealerCode).isBlank()
                        || row.get("dealerCode").equals(String.valueOf(dealerCode)))
                .toList();

        List<Map<String, Object>> projected = filtered.stream()
                .map(row -> project(row, request.effectiveColumns()))
                .toList();

        // Pagination is UI-controlled for DEALER_LEDGER (reports/dealer-ledge/
        // dealer-ledger-pagination-2026-09-16_170000.md) — return every row in
        // one page instead of slicing by request.page()/pageSize().

        BigDecimal totalDebit = filtered.stream()
                .map(row -> (BigDecimal) row.get("debit"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = filtered.stream()
                .map(row -> (BigDecimal) row.get("credit"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> totals = Map.of("debit", totalDebit, "credit", totalCredit);

        return new ReportQueryResult(projected, totals, projected.size(), Instant.now(), 5);
    }

    private List<Map<String, Object>> buildRows() {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        rows.add(balanceRow(null, "DEALER OPENING BALANCE", "0.00", "0.00"));
        rows.add(balanceRow(null, "VEHICLE ACCOUNT OPENING BALANCE", "0.00", "862320.82"));

        rows.add(docRow("DR", "1800367456", "29-05-2026", "20260529", null,
                "20260529- FALLP Outstanding Transfer", "5877.89", "0.00"));
        rows.add(docRow("DR", "1800367459", "01-06-2026", "20260601", null,
                "20260601- FALLP Outstanding Transfer", "711.76", "0.00"));
        rows.add(docRow("DR", "1800367457", "07-06-2026", "20260607", null,
                "20260607- FALLP Outstanding Transfer", "10261.28", "0.00"));
        rows.add(docRow("DR", "1800367461", "09-06-2026", "20260609", null,
                "20260609- FALLP Outstanding Transfer", "2554.11", "0.00"));
        rows.add(docRow("DR", "1800367452", "09-06-2026", "20260609", null,
                "20260609- FALLP Outstanding Transfer", "479.91", "0.00"));
        rows.add(docRow("DR", "1800367455", "14-06-2026", "20260614", null,
                "20260614- FALLP Outstanding Transfer", "6443.51", "0.00"));
        rows.add(docRow("DR", "1800367460", "17-06-2026", "20260617", null,
                "20260617- FALLP Outstanding Transfer", "19720.16", "0.00"));
        rows.add(docRow("DR", "1800367458", "18-06-2026", "20260618", null,
                "20260618- FALLP Outstanding Transfer", "16074.90", "0.00"));
        rows.add(docRow("DR", "1800367453", "18-06-2026", "20260618", null,
                "20260618- FALLP Outstanding Transfer", "30617.46", "0.00"));
        rows.add(docRow("DR", "1800367454", "10-06-2026", "9801032893", null,
                "9801049356- HSRP FROM ZFAM", "45468.00", "0.00"));

        rows.add(docRow("DA", "1600594164", "12-08-2026", "20260812",
                "IDIB000V029IDIBR52026081203873703", null, "0.00", "1600000.00"));
        rows.add(docRow("DA", "1600596441", "12-08-2026", "20260812",
                "IDIBH22454275500", null, "0.00", "300000.00"));
        rows.add(docRow("DR", "1800335155", "12-08-2026", "20260813",
                "AP - TVS TECHLINEX APP DEBIT", null, "7670.00", "0.00"));
        rows.add(docRow("DR", "1800337848", "13-08-2026", null,
                "AP-SALES NPS TELE CALLING CHARGES JULY'26", null, "2152.00", "0.00"));

        rows.add(docRow("RV", "90885573", "16-08-2026", "1501116247", null,
                "TVS RAIDER - OBDIIB DISC SS ES+KS M.GREY", "981172.91", "0.00"));
        rows.add(docRow("RV", "90885575", "16-08-2026", "1501116248", null,
                "TVS XL100 HEAVYDUTY ITS -ALLOY DP MB+BLK", "592536.41", "0.00"));
        rows.add(docRow("RV", "90885798", "16-08-2026", "20260816", null,
                "SIDE STAND SWITCH", "12205.80", "0.00"));
        rows.add(docRow("RV", "90885799", "16-08-2026", "20260816", null,
                "BRKT CRASHGUARD MTG", "2925.93", "0.00"));

        rows.add(docRow("DA", "1600602253", "16-08-2026", "20260816",
                "IDIB000B027IDIBH22854650337", null, "0.00", "600000.00"));
        rows.add(docRow("DR", "1800345289", "17-08-2026", "9801057795", null,
                "9801057795- HSRP FROM ZFAM", "7040.10", "0.00"));
        rows.add(docRow("DA", "1600607240", "18-08-2026", "20260818",
                "AMOUNT TRANSFER FROM ZEVC TO ZTS1", null, "0.00", "270000.00"));
        rows.add(docRow("DA", "1600612613", "19-08-2026", "9900249092",
                "9900249092- MAND FROM ZFAM", null, "0.00", "2000000.00"));
        rows.add(docRow("DR", "1800349909", "19-08-2026", "9900249092", null,
                "9900249092- MAND FROM ZFAM", "15638.54", "0.00"));

        rows.add(docRow("RV", "90910814", "20-08-2026", "1501118682", null,
                "TVS RAIDER - OBDIIB DISC IGO SEDMA WK BK", "3098597.37", "0.00"));
        rows.add(docRow("RV", "90912203", "21-08-2026", "20260821", null,
                "SAREE GUARD RAIDER", "19210.56", "0.00"));

        rows.add(docRow("DR", "1800358877", "22-08-2026", "9801067613", null,
                "9801067613- HSRP FROM ZFAM", "292.35", "0.00"));
        rows.add(docRow("DR", "1800357153", "23-08-2026", "9900250394", null,
                "9900250394- MAND FROM ZFAM", "9480.12", "0.00"));
        rows.add(docRow("DR", "1800357154", "23-08-2026", "9990771088", null,
                "9990771088- MAND FROM ZFAM", "3115.20", "0.00"));
        rows.add(docRow("DR", "1800365239", "25-08-2026", "9900251279", null,
                "9900251279- MAND FROM ZFAM", "12536.32", "0.00"));
        rows.add(docRow("DR", "1800370984", "26-08-2026", "9801073491", null,
                "9801073491- HSRP FROM ZFAM", "4476.03", "0.00"));

        rows.add(docRow("DA", "1600652243", "27-08-2026", "20260827",
                "IDIB000V029IDIBR52026082704149497", null, "0.00", "1800000.00"));

        rows.add(docRow("RV", "90962104", "29-08-2026", "1501124016", null,
                "TVS JUPITER 125-OBDIIB DISC DT SXC MG+PW", "1632703.55", "0.00"));
        rows.add(docRow("RV", "90962562", "29-08-2026", "1501124078", null,
                "TVS RAIDER - OBDIIB DISC SS ES+KS RA RED", "335550.51", "0.00"));
        rows.add(docRow("RV", "90962878", "29-08-2026", "20260829", null,
                "SAREE GUARD RAIDER", "2474.32", "0.00"));
        rows.add(docRow("RV", "90963470", "29-08-2026", "20260829", null,
                "BEEPER", "1246.31", "0.00"));

        rows.add(docRow("DA", "1600656401", "29-08-2026", "20260829",
                "IDIB000B027IDIBH24156043165", null, "0.00", "500000.00"));
        rows.add(docRow("DR", "1600658277", "30-08-2026", "20260830", null,
                "AMOUNT TRANSFERRED FROM ZTS1 TO ZTS2", "7000.00", "0.00"));
        rows.add(docRow("DA", "1600675635", "31-08-2026", "20260831", null,
                "AMOUNT TRANSFERRED FROM ZTS1 TO ZEVC", "0.00", "150000.00"));
        rows.add(docRow("DR", "1800386116", "31-08-2026", "9900253705", null,
                "9900253705- MAND FROM ZFAM", "23590.56", "0.00"));

        return rows;
    }

    private Map<String, Object> balanceRow(String cca, String textDec, String debit, String credit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dealerCode", DEALER_CODE);
        row.put("docType", null);
        row.put("docReferenceNo", null);
        row.put("docDate", null);
        row.put("assignment", null);
        row.put("cca", cca != null ? cca : CCA);
        row.put("textDec", textDec);
        row.put("vehicleNarration", null);
        row.put("debit", new BigDecimal(debit));
        row.put("credit", new BigDecimal(credit));
        row.put("cblRefNo", null);
        row.put("oeRefNo", null);
        row.put("spRefNo", null);
        row.put("acRefNo", null);
        row.put("evRefNo", null);
        row.put("acwshRefNo", null);
        return row;
    }

    private Map<String, Object> docRow(String docType, String docReferenceNo, String docDate, String assignment,
                                        String textDec, String vehicleNarration, String debit, String credit) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dealerCode", DEALER_CODE);
        row.put("docType", docType);
        row.put("docReferenceNo", docReferenceNo);
        row.put("docDate", docDate);
        row.put("assignment", assignment);
        row.put("cca", CCA);
        row.put("textDec", textDec);
        row.put("vehicleNarration", vehicleNarration);
        row.put("debit", new BigDecimal(debit));
        row.put("credit", new BigDecimal(credit));
        row.put("cblRefNo", "RV".equals(docType) ? "CBL-" + docReferenceNo : null);
        row.put("oeRefNo", null);
        row.put("spRefNo", null);
        row.put("acRefNo", null);
        row.put("evRefNo", null);
        row.put("acwshRefNo", null);
        return row;
    }

    private ReportQueryResult executeWarrantyCost(ReportQueryRequest request) {
        List<Map<String, Object>> allRows = buildWarrantyCostRows();

        Object dealerCode = request.parameters().get("dealerCode");
        Object claimDate = request.parameters().get("claimDate");
        LocalDate from = claimDateBound(claimDate, "from");
        LocalDate to = claimDateBound(claimDate, "to");

        List<Map<String, Object>> filtered = allRows.stream()
                .filter(row -> dealerCode == null || String.valueOf(dealerCode).isBlank()
                        || row.get("dealerCode").equals(String.valueOf(dealerCode)))
                .filter(row -> {
                    LocalDate rowDate = LocalDate.parse((String) row.get("claimDate"), ROW_DATE_FORMAT);
                    return (from == null || !rowDate.isBefore(from)) && (to == null || !rowDate.isAfter(to));
                })
                .toList();

        BigDecimal totalLabor = filtered.stream()
                .map(row -> (BigDecimal) row.get("laborCost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPart = filtered.stream()
                .map(row -> (BigDecimal) row.get("partCost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = filtered.stream()
                .map(row -> (BigDecimal) row.get("totalCost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> totals = Map.of("laborCost", totalLabor, "partCost", totalPart, "totalCost", totalCost);

        int fromIndex = Math.min((request.page() - 1) * request.pageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + request.pageSize(), filtered.size());
        List<Map<String, Object>> paged = filtered.subList(fromIndex, toIndex);

        List<Map<String, Object>> projected = paged.stream()
                .map(row -> project(row, request.effectiveColumns()))
                .toList();

        return new ReportQueryResult(projected, totals, filtered.size(), Instant.now(), 4);
    }

    @SuppressWarnings("unchecked")
    private LocalDate claimDateBound(Object claimDate, String bound) {
        if (!(claimDate instanceof Map<?, ?> range)) {
            return null;
        }
        Object value = ((Map<String, Object>) range).get(bound);
        return value == null ? null : LocalDate.parse(String.valueOf(value));
    }

    private List<Map<String, Object>> buildWarrantyCostRows() {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        rows.add(warrantyCostRow("WC26050001", "05-05-2026", "P-10023", "CLUTCH PLATE ASSY", "450.00", "1250.00"));
        rows.add(warrantyCostRow("WC26051204", "18-05-2026", "P-10456", "FRONT BRAKE SHOE", "300.00", "780.50"));
        rows.add(warrantyCostRow("WC26060342", "02-06-2026", "P-11002", "HEADLIGHT ASSEMBLY", "600.00", "2100.00"));
        rows.add(warrantyCostRow("WC26061890", "20-06-2026", "P-10789", "SIDE STAND SWITCH", "150.00", "320.75"));
        rows.add(warrantyCostRow("WC26070456", "10-07-2026", "P-11234", "CDI UNIT", "500.00", "1890.00"));
        rows.add(warrantyCostRow("WC26071123", "25-07-2026", "P-10567", "REAR SHOCK ABSORBER", "700.00", "3200.00"));
        rows.add(warrantyCostRow("WC26080234", "05-08-2026", "P-11890", "SPEEDOMETER CABLE", "100.00", "410.25"));
        rows.add(warrantyCostRow("WC26081567", "22-08-2026", "P-10234", "CARBURETOR ASSY", "800.00", "4500.00"));
        return rows;
    }

    private Map<String, Object> warrantyCostRow(String claimNo, String claimDate, String partNo,
                                                 String partDescription, String laborCost, String partCost) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dealerCode", DEALER_CODE);
        row.put("dealerName", "PAWAN SARKAR AUTOMOBILES");
        row.put("claimNo", claimNo);
        row.put("claimDate", claimDate);
        row.put("partNo", partNo);
        row.put("partDescription", partDescription);
        BigDecimal labor = new BigDecimal(laborCost);
        BigDecimal part = new BigDecimal(partCost);
        row.put("laborCost", labor);
        row.put("partCost", part);
        row.put("totalCost", labor.add(part));
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
