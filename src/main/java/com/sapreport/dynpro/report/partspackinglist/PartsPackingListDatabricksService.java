package com.sapreport.dynpro.report.partspackinglist;

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
 * Runs the PARTS_PACKING_LIST query (same {@code fn_parts_packing_list} function as
 * {@code PartsPackingListQueryRunner.java} / {@code databricks-query/parts-packing-list.sql})
 * directly against Databricks, for {@code POST /parts-packing-list/fetch-data-bricks-data}.
 */
@Service
public class PartsPackingListDatabricksService {

    private static final String QUERY = "SELECT * FROM sap_dynpro.bumblebee.fn_parts_packing_list(?, ?, ?) LIMIT ?";

    private final String serverHostname;
    private final String httpPath;
    private final String clientId;
    private final String clientSecret;

    public PartsPackingListDatabricksService(
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
            throw new PartsPackingListDatabricksQueryException("Parts packing list Databricks query failed: " + e.getMessage(), e);
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
