package com.sapreport.dynpro.report.databricks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Java CLI equivalent of {@code run_query.py}, since Python isn't runnable
 * in this environment. Connects to a Databricks SQL Warehouse via the
 * {@code com.databricks:databricks-jdbc} driver using OAuth M2M (service
 * principal client_credentials flow) and runs a query passed as the first
 * argument.
 *
 * <p>Credentials are read directly from the process environment (set them
 * in your shell / IDE run config / deployment platform's secret store):
 * DATABRICKS_SERVER_HOSTNAME, DATABRICKS_HTTP_PATH, DATABRICKS_CLIENT_ID,
 * DATABRICKS_CLIENT_SECRET. No credentials file is used or committed.
 *
 * <p>Usage: {@code mvn -q compile exec:java
 * -Dexec.mainClass=com.sapreport.dynpro.report.databricks.DatabricksQueryRunner
 * -Dexec.args="SELECT 1 AS test_col"}
 */
public final class DatabricksQueryRunner {

    private DatabricksQueryRunner() {
    }

    public static void main(String[] args) {
        String serverHostname = requireEnv("DATABRICKS_SERVER_HOSTNAME");
        String httpPath = requireEnv("DATABRICKS_HTTP_PATH");
        String clientId = requireEnv("DATABRICKS_CLIENT_ID");
        String clientSecret = requireEnv("DATABRICKS_CLIENT_SECRET");

        String query = args.length > 0 ? args[0] : "SELECT 1 AS test_col";

        String jdbcUrl = "jdbc:databricks://%s:443/default;httpPath=%s;AuthMech=11;Auth_Flow=1;OAuth2ClientId=%s;OAuth2Secret=%s;ssl=1"
                .formatted(serverHostname, httpPath, clientId, clientSecret);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            printResults(resultSet);
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
