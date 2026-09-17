package com.sapreport.dynpro.report.api;

import com.sapreport.dynpro.report.action.ReportRowStore;
import com.sapreport.dynpro.report.action.RowIdentity;
import com.sapreport.dynpro.report.condition.ConditionEvaluator;
import com.sapreport.dynpro.report.exception.ConfigVersionMismatchException;
import com.sapreport.dynpro.report.exception.ReportAccessDeniedException;
import com.sapreport.dynpro.report.exception.ReportNotFoundException;
import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.ColumnGroup;
import com.sapreport.dynpro.report.metadata.PagingConfig;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import com.sapreport.dynpro.report.query.ReportQueryExecutor;
import com.sapreport.dynpro.report.query.ReportQueryRequest;
import com.sapreport.dynpro.report.query.ReportQueryResult;
import com.sapreport.dynpro.report.query.SortSpec;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import com.sapreport.dynpro.report.validation.SortValidator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates the two report endpoints for every report: metadata lookup,
 * validation, {@code visibleWhen} evaluation, and query execution. One
 * generic implementation, not one per report — see reports-overview.md §5.
 */
@Service
public class ReportService {

    private final ReportMetadataRepository metadataRepository;
    private final ReportParameterValidator parameterValidator;
    private final SortValidator sortValidator;
    private final ReportQueryExecutor queryExecutor;
    private final ReportCallerContext callerContext;
    private final ReportRowStore rowStore;

    public ReportService(ReportMetadataRepository metadataRepository,
                          ReportParameterValidator parameterValidator,
                          SortValidator sortValidator,
                          ReportQueryExecutor queryExecutor,
                          ReportCallerContext callerContext,
                          ReportRowStore rowStore) {
        this.metadataRepository = metadataRepository;
        this.parameterValidator = parameterValidator;
        this.sortValidator = sortValidator;
        this.queryExecutor = queryExecutor;
        this.callerContext = callerContext;
        this.rowStore = rowStore;
    }

    public ReportConfigResponse getConfig(String reportCode) {
        ReportMetadata metadata = requireMetadata(reportCode);
        ReportContext context = new ReportContext(callerContext.dealerCode(), callerContext.dealerDescription());
        return new ReportConfigResponse(metadata.reportCode(), metadata.title(), metadata.configVersion(), context,
                metadata.parameters(), metadata.columnGroups(), metadata.export(), metadata.paging(),
                metadata.reportGroup(), metadata.groupLabel(), metadata.groupOrder(), metadata.parameterGroups(),
                metadata.dateRangeConstraints(), metadata.rowKey(), metadata.actions());
    }

    public ReportDataResponse getData(String reportCode, ReportDataRequest request) {
        ReportMetadata metadata = requireMetadata(reportCode);

        if (!metadata.configVersion().equals(request.configVersion())) {
            throw new ConfigVersionMismatchException(metadata.configVersion(), request.configVersion());
        }

        Map<String, Object> parameters = parameterValidator.validateAndCoerce(metadata, request.parameters());
        enforceCompanyCodeScope(parameters);

        List<EffectiveColumn> effectiveColumns = computeEffectiveColumns(metadata, parameters);
        List<String> effectiveColumnNames = effectiveColumns.stream().map(EffectiveColumn::columnName).toList();
        Map<String, ColumnDefinition> columnsByField = metadata.columnGroups().stream()
                .flatMap(group -> group.columns().stream())
                .collect(Collectors.toMap(ColumnDefinition::field, column -> column, (a, b) -> a));

        List<SortSpec> sort = request.sort() == null ? List.of() : request.sort();
        sortValidator.validate(sort, effectiveColumnNames, columnsByField);

        int pageSize = clampPageSize(request.paging(), metadata.paging());
        int page = request.paging() != null && request.paging().page() != null
                ? Math.max(request.paging().page(), 1)
                : 1;

        ReportQueryRequest queryRequest = new ReportQueryRequest(
                reportCode, parameters, effectiveColumnNames, sort, page, pageSize);
        ReportQueryResult result = queryExecutor.execute(queryRequest);

        List<Map<String, Object>> rows = attachRowIdentity(metadata, result.rows());

        // If the executor returned every row instead of slicing (e.g. UI-controlled
        // pagination — reports/dealer-ledge/dealer-ledger-pagination-2026-09-16_170000.md),
        // report a single page covering all rows rather than the requested pageSize.
        int effectivePageSize = result.rows().size() == result.totalRows() && result.totalRows() > 0
                ? (int) result.totalRows()
                : pageSize;
        int totalPages = effectivePageSize == 0 ? 0 : (int) Math.ceil(result.totalRows() / (double) effectivePageSize);
        PagingResponse pagingResponse = new PagingResponse(page, effectivePageSize, result.totalRows(), totalPages);
        ResponseMeta meta = new ResponseMeta(Instant.now(), result.dataAsOf(), result.queryMs());

        return new ReportDataResponse(metadata.reportCode(), metadata.configVersion(), effectiveColumns,
                rows, result.totals(), pagingResponse, meta);
    }

    /**
     * When a report declares {@code rowKey}, every row gets a computed
     * {@code _rowKey}/{@code _rowVersion} and is seeded into the
     * {@link ReportRowStore} so a subsequent action referencing that row can
     * find its current state and check {@code rowVersion}. Reports without
     * {@code rowKey} (e.g. DEALER_LEDGER) are returned unchanged.
     */
    private List<Map<String, Object>> attachRowIdentity(ReportMetadata metadata, List<Map<String, Object>> rows) {
        if (metadata.rowKey() == null) {
            return rows;
        }
        List<ColumnDefinition> editableColumns = metadata.columnGroups().stream()
                .flatMap(group -> group.columns().stream())
                .filter(ColumnDefinition::editable)
                .toList();
        return rows.stream().map(row -> {
            String rowKey = RowIdentity.computeRowKey(metadata.rowKey(), row);
            String rowVersion = RowIdentity.computeRowVersion(editableColumns, row);
            rowStore.save(metadata.reportCode(), rowKey, row);
            Map<String, Object> withIdentity = new LinkedHashMap<>(row);
            withIdentity.put("_rowKey", rowKey);
            withIdentity.put("_rowVersion", rowVersion);
            return withIdentity;
        }).toList();
    }

    private void enforceCompanyCodeScope(Map<String, Object> parameters) {
        Object companyCode = parameters.get("companyCode");
        if (companyCode == null) {
            return;
        }
        if (!callerContext.authorizedCompanyCodes().contains(String.valueOf(companyCode))) {
            throw new ReportAccessDeniedException(String.valueOf(companyCode));
        }
    }

    private List<EffectiveColumn> computeEffectiveColumns(ReportMetadata metadata, Map<String, Object> parameters) {
        List<EffectiveColumn> columns = new ArrayList<>();
        for (ColumnGroup group : metadata.columnGroups()) {
            if (ConditionEvaluator.evaluate(group.visibleWhen(), parameters)) {
                for (ColumnDefinition column : group.columns()) {
                    columns.add(new EffectiveColumn(column.field(), column.defaultVisible(), column.visible()));
                }
            }
        }
        return columns;
    }

    private int clampPageSize(PagingRequest requested, PagingConfig config) {
        int requestedSize = requested != null && requested.pageSize() != null
                ? requested.pageSize()
                : config.defaultPageSize();
        if (requestedSize <= 0) {
            requestedSize = config.defaultPageSize();
        }
        return Math.min(requestedSize, config.maxPageSize());
    }

    private ReportMetadata requireMetadata(String reportCode) {
        return metadataRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new ReportNotFoundException(reportCode));
    }
}
