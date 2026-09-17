package com.sapreport.dynpro.report.dealerledger;

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
 * Runs the DEALER_LEDGER query (same {@code f_dealer_ledger} function as
 * {@code DealerLedgerQueryRunner.java} / {@code databricks-query/dealer-ledger.sql})
 * directly against Databricks, for the standalone {@code /dealer-ledger/fetchDatabricksdata}
 * endpoint. A one-off, Spring-managed counterpart to that CLI class.
 */
@Service
public class DealerLedgerDatabricksService {

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

    private final String serverHostname;
    private final String httpPath;
    private final String clientId;
    private final String clientSecret;

    public DealerLedgerDatabricksService(
            @Value("${databricks.server-hostname}") String serverHostname,
            @Value("${databricks.http-path}") String httpPath,
            @Value("${databricks.client-id}") String clientId,
            @Value("${databricks.client-secret}") String clientSecret) {
        this.serverHostname = serverHostname;
        this.httpPath = httpPath;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<Map<String, Object>> fetch(String bukrs, String kunnr, LocalDate fromDate, int limit) {
        String jdbcUrl = "jdbc:databricks://%s:443/default;httpPath=%s;AuthMech=11;Auth_Flow=1;OAuth2ClientId=%s;OAuth2Secret=%s;ssl=1"
                .formatted(serverHostname, httpPath, clientId, clientSecret);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            statement.setString(1, bukrs);
            statement.setString(2, kunnr);
            statement.setString(3, fromDate.toString());
            statement.setString(4, fromDate.toString());
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                return toRows(resultSet);
            }
        } catch (SQLException e) {
            throw new DealerLedgerDatabricksQueryException("Dealer ledger Databricks query failed: " + e.getMessage(), e);
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
