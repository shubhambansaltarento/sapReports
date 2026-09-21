package com.sapreport.dynpro.report.databricks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Runs the DEALER_LEDGER query (see
 * {@code src/main/java/com/sapreport/dynpro/report/databricks-query/dealer-ledger.sql})
 * against the Databricks SQL Warehouse, with bukrs/kunnr/fromDate as typed
 * Java args instead of a raw SQL string. The query's one-month window end is
 * derived server-side ({@code ADD_MONTHS(from_date, 1)}), so only the start
 * date is a parameter here.
 *
 * <p>Credentials are read directly from the process environment (set them
 * in your shell / IDE run config / deployment platform's secret store):
 * DATABRICKS_SERVER_HOSTNAME, DATABRICKS_HTTP_PATH, DATABRICKS_CLIENT_ID,
 * DATABRICKS_CLIENT_SECRET.
 *
 * <p>Usage: {@code java DealerLedgerQueryRunner <bukrs> <kunnr> <fromDate> [limit]}
 * e.g. {@code java DealerLedgerQueryRunner 1000 0000010015 2026-07-17 100}
 */
public final class DealerLedgerQueryRunner {

    private static final String QUERY = """
            SELECT *
            FROM sap_dynpro.bumblebee.f_dealer_ledger(
                    CAST(? AS STRING),
                    CAST(? AS STRING),
                    CAST(? AS DATE),
                    ADD_MONTHS(CAST(? AS DATE), 1)
                 )
            ORDER BY credit_control_area, sort_grp, post_date, doc_reference_no, line_item
            LIMIT ?
            """;

    private DealerLedgerQueryRunner() {
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Usage: DealerLedgerQueryRunner <bukrs> <kunnr> <fromDate> [limit]");
            System.exit(1);
        }
        String bukrs = args[0];
        String kunnr = args[1];
        String fromDate = args[2];
        int limit = args.length == 4 ? Integer.parseInt(args[3]) : 100;

        String serverHostname = requireEnv("DATABRICKS_SERVER_HOSTNAME");
        String httpPath = requireEnv("DATABRICKS_HTTP_PATH");
        String clientId = requireEnv("DATABRICKS_CLIENT_ID");
        String clientSecret = requireEnv("DATABRICKS_CLIENT_SECRET");

        String jdbcUrl = "jdbc:databricks://%s:443/default;httpPath=%s;AuthMech=11;Auth_Flow=1;OAuth2ClientId=%s;OAuth2Secret=%s;ssl=1"
                .formatted(serverHostname, httpPath, clientId, clientSecret);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, bukrs);
            statement.setString(2, kunnr);
            statement.setString(3, fromDate);
            statement.setString(4, fromDate);
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                printResults(resultSet);
            }
        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printResults(ResultSet resultSet) throws Exception {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                header.append(" | ");
            }
            header.append(metaData.getColumnLabel(i));
        }
        System.out.println(header);
        System.out.println("-".repeat(header.length()));

        while (resultSet.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                row.append(resultSet.getString(i));
            }
            System.out.println(row);
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
