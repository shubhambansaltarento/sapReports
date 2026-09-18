package com.sapreport.dynpro.report.warrantycost;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs the WARRANTY_COST query (same {@code api_warranty_cost} function as
 * {@code WarrantyCostQueryRunner.java} / {@code databricks-query/warranty-cost.sql})
 * directly against Databricks, for {@code POST /warranty-cost/fetch-data-bricks-data}.
 */
@Service
public class WarrantyCostDatabricksService {

    private static final String QUERY = "SELECT * FROM sap_dynpro.bumblebee.api_warranty_cost(?, ?, ?) LIMIT ?";

    private final String serverHostname;
    private final String httpPath;
    private final String clientId;
    private final String clientSecret;

    public WarrantyCostDatabricksService(
            @Value("${databricks.server-hostname}") String serverHostname,
            @Value("${databricks.http-path}") String httpPath,
            @Value("${databricks.client-id}") String clientId,
            @Value("${databricks.client-secret}") String clientSecret) {
        this.serverHostname = serverHostname;
        this.httpPath = httpPath;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<Map<String, Object>> fetch(String dealerCode, LocalDate fromDate, LocalDate toDate, int limit) {
        String jdbcUrl = "jdbc:databricks://%s:443/default;httpPath=%s;AuthMech=11;Auth_Flow=1;OAuth2ClientId=%s;OAuth2Secret=%s;ssl=1"
                .formatted(serverHostname, httpPath, clientId, clientSecret);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, dealerCode);
            statement.setString(2, fromDate.toString());
            statement.setString(3, toDate.toString());
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return toRows(resultSet);
            }
        } catch (SQLException e) {
            throw new WarrantyCostDatabricksQueryException("Warranty cost Databricks query failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> toRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
