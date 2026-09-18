package com.sapreport.dynpro.report.databricks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Runs the PARTS_PACKING_LIST query (see
 * {@code src/main/java/com/sapreport/dynpro/report/databricks-query/parts-packing-list.sql})
 * against the Databricks SQL Warehouse, with dealerCode/fromDate/toDate as
 * typed Java args instead of a raw SQL string.
 *
 * <p>Credentials are read directly from the process environment (set them
 * in your shell / IDE run config / deployment platform's secret store):
 * DATABRICKS_SERVER_HOSTNAME, DATABRICKS_HTTP_PATH, DATABRICKS_CLIENT_ID,
 * DATABRICKS_CLIENT_SECRET.
 *
 * <p>Usage: {@code java PartsPackingListQueryRunner <dealerCode> <fromDate> <toDate> [limit]}
 * e.g. {@code java PartsPackingListQueryRunner 0000010015 2026-08-01 2026-09-01 100}
 */
public final class PartsPackingListQueryRunner {

    private static final String QUERY = "SELECT * FROM sap_dynpro.bumblebee.fn_parts_packing_list(?, ?, ?) LIMIT ?";

    private PartsPackingListQueryRunner() {
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Usage: PartsPackingListQueryRunner <dealerCode> <fromDate> <toDate> [limit]");
            System.exit(1);
        }
        String dealerCode = args[0];
        String fromDate = args[1];
        String toDate = args[2];
        int limit = args.length == 4 ? Integer.parseInt(args[3]) : 100;

        String serverHostname = requireEnv("DATABRICKS_SERVER_HOSTNAME");
        String httpPath = requireEnv("DATABRICKS_HTTP_PATH");
        String clientId = requireEnv("DATABRICKS_CLIENT_ID");
        String clientSecret = requireEnv("DATABRICKS_CLIENT_SECRET");

        String jdbcUrl = "jdbc:databricks://%s:443/default;httpPath=%s;AuthMech=11;Auth_Flow=1;OAuth2ClientId=%s;OAuth2Secret=%s;ssl=1"
                .formatted(serverHostname, httpPath, clientId, clientSecret);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, dealerCode);
            statement.setString(2, fromDate);
            statement.setString(3, toDate);
            statement.setInt(4, limit);
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
