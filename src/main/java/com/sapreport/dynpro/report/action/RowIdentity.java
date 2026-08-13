package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.RowKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes a row's {@code _rowKey} (its {@link RowKeySpec} fields joined by
 * the configured separator) and {@code _rowVersion} (a content hash of its
 * current editable-column values). There is no real "last modified"
 * timestamp source yet (no data source is wired in — see
 * {@code jdbc-connection.md}), so the version is deliberately derived only
 * from the values that can actually change; any edit to an editable column
 * changes the hash, which is exactly what optimistic-concurrency checks
 * need. Swap in a timestamp-based scheme once a real source exists.
 */
public final class RowIdentity {

    private RowIdentity() {
    }

    public static String computeRowKey(RowKeySpec spec, Map<String, Object> row) {
        return spec.fields().stream()
                .map(field -> String.valueOf(row.get(field)))
                .collect(Collectors.joining(spec.separator()));
    }

    public static String computeRowVersion(Collection<ColumnDefinition> editableColumns, Map<String, Object> row) {
        String fingerprint = editableColumns.stream()
                .map(ColumnDefinition::field)
                .sorted()
                .map(field -> field + "=" + row.get(field))
                .collect(Collectors.joining("|"));
        return "v1:" + sha256Hex(fingerprint);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
