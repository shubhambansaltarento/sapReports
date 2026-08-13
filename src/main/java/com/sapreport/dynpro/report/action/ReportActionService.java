package com.sapreport.dynpro.report.action;

import com.sapreport.dynpro.report.condition.ConditionEvaluator;
import com.sapreport.dynpro.report.exception.ReportNotFoundException;
import com.sapreport.dynpro.report.metadata.ActionDefinition;
import com.sapreport.dynpro.report.metadata.ActionScope;
import com.sapreport.dynpro.report.metadata.ColumnDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import com.sapreport.dynpro.report.security.ReportCallerContext;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import com.sapreport.dynpro.report.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates {@code POST /api/v1/reports/{reportCode}/actions/{actionKey}}:
 * request-level validation (configVersion, unknown action, idempotency-key
 * presence), idempotent replay, and per-row rowVersion/dispatch — the same
 * "one generic engine, not one per report" shape as {@link
 * com.sapreport.dynpro.report.api.ReportService}. Business logic for a
 * specific report+action lives entirely in its {@link ReportActionHandler}.
 */
@Service
public class ReportActionService {

    private final ReportMetadataRepository metadataRepository;
    private final ReportParameterValidator parameterValidator;
    private final ReportActionHandlerRegistry handlerRegistry;
    private final ReportRowStore rowStore;
    private final IdempotencyStore idempotencyStore;
    private final ReportCallerContext callerContext;

    public ReportActionService(ReportMetadataRepository metadataRepository,
                                ReportParameterValidator parameterValidator,
                                ReportActionHandlerRegistry handlerRegistry,
                                ReportRowStore rowStore,
                                IdempotencyStore idempotencyStore,
                                ReportCallerContext callerContext) {
        this.metadataRepository = metadataRepository;
        this.parameterValidator = parameterValidator;
        this.handlerRegistry = handlerRegistry;
        this.rowStore = rowStore;
        this.idempotencyStore = idempotencyStore;
        this.callerContext = callerContext;
    }

    public ActionResponse execute(String reportCode, String actionKey, ActionRequest request) {
        ReportMetadata metadata = metadataRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new ReportNotFoundException(reportCode));

        ActionDefinition action = metadata.actions().stream()
                .filter(a -> a.actionKey().equals(actionKey))
                .findFirst()
                .orElseThrow(() -> new ValidationException(List.of(new ValidationError("actionKey",
                        ValidationErrorCodes.UNKNOWN_ACTION,
                        "Unknown action '" + actionKey + "' for report '" + reportCode + "'"))));

        if (action.scope() == ActionScope.CLIENT_ONLY) {
            throw new ValidationException(List.of(new ValidationError("actionKey",
                    ValidationErrorCodes.CLIENT_ONLY_ACTION,
                    "Action '" + actionKey + "' is client-only and has no server endpoint")));
        }

        if (!metadata.configVersion().equals(request.configVersion())) {
            throw new ValidationException(List.of(new ValidationError("configVersion",
                    ValidationErrorCodes.CONFIG_VERSION_MISMATCH,
                    "configVersion mismatch: expected '" + metadata.configVersion() + "' but received '"
                            + request.configVersion() + "'")));
        }

        boolean hasIdempotencyKey = request.idempotencyKey() != null && !request.idempotencyKey().isBlank();
        if (action.sideEffecting() && !hasIdempotencyKey) {
            throw new ValidationException(List.of(new ValidationError("idempotencyKey", ValidationErrorCodes.REQUIRED,
                    "idempotencyKey is required for side-effecting action '" + actionKey + "'")));
        }

        if (action.scope() == ActionScope.ROW && request.rows().size() != 1) {
            throw new ValidationException(List.of(new ValidationError("rows", ValidationErrorCodes.INVALID_ROW_COUNT,
                    "Action scope ROW requires exactly one row")));
        }

        if (action.sideEffecting() && hasIdempotencyKey) {
            Optional<ActionResponse> replay = idempotencyStore.find(reportCode, actionKey, request.idempotencyKey());
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        ReportActionHandler handler = handlerRegistry.resolve(reportCode, actionKey)
                .orElseThrow(() -> new IllegalStateException(
                        "No ReportActionHandler registered for " + reportCode + "/" + actionKey));

        // Same validation/coercion the data endpoint applies to its search parameters — e.g. this is what
        // collapses a divisionVehicle/divisionSpares checkbox pair into one internal `division` value.
        Map<String, Object> parameters = parameterValidator.validateAndCoerce(metadata, request.parameters());

        List<ColumnDefinition> editableColumns = metadata.columnGroups().stream()
                .flatMap(group -> group.columns().stream())
                .filter(ColumnDefinition::editable)
                .toList();

        List<RowActionResult> results = new ArrayList<>();
        for (RowChangeRequest rowChange : request.rows()) {
            results.add(processRow(reportCode, action, editableColumns, handler, parameters, rowChange, request.idempotencyKey()));
        }

        ActionSummary summary = new ActionSummary(
                results.size(),
                (int) results.stream().filter(r -> r.status() == RowActionStatus.SUCCESS).count(),
                (int) results.stream().filter(r -> r.status() != RowActionStatus.SUCCESS).count());

        ActionResponse response = new ActionResponse(actionKey, summary, results);

        if (action.sideEffecting() && hasIdempotencyKey) {
            idempotencyStore.save(reportCode, actionKey, request.idempotencyKey(), response);
        }
        return response;
    }

    private RowActionResult processRow(String reportCode, ActionDefinition action, List<ColumnDefinition> editableColumns,
                                        ReportActionHandler handler, Map<String, Object> parameters, RowChangeRequest rowChange,
                                        String idempotencyKey) {
        // "No changes" only means "nothing to do" for bulk edit-and-save actions (appliesTo EDITED_ROWS).
        // Trigger-style actions (downloadInvoicePdf, initiateESign, pushESignToTvsm, ...) have no natural
        // "changes" payload at all — they must still reach the handler.
        if ("EDITED_ROWS".equals(action.appliesTo()) && rowChange.changes().isEmpty()) {
            return new RowActionResult(rowChange.rowKey(), RowActionStatus.SKIPPED, rowChange.rowVersion(), List.of());
        }

        Optional<Map<String, Object>> current = rowStore.find(reportCode, rowChange.rowKey());
        if (current.isEmpty()) {
            return new RowActionResult(rowChange.rowKey(), RowActionStatus.FAILED, null, List.of(
                    new ValidationError("rowKey", ValidationErrorCodes.ROW_NOT_FOUND, "No such row: " + rowChange.rowKey())));
        }

        String currentVersion = RowIdentity.computeRowVersion(editableColumns, current.get());
        if (!currentVersion.equals(rowChange.rowVersion())) {
            return new RowActionResult(rowChange.rowKey(), RowActionStatus.STALE, currentVersion, List.of(
                    new ValidationError("rowVersion", ValidationErrorCodes.ROW_VERSION_CONFLICT,
                            "Row has been modified since it was last read")));
        }

        // Re-check enabledWhen server-side — the UI-side gate is just an affordance, never trusted alone.
        if (action.enabledWhen() != null && !ConditionEvaluator.evaluate(action.enabledWhen(), current.get())) {
            return new RowActionResult(rowChange.rowKey(), RowActionStatus.FAILED, currentVersion, List.of(
                    new ValidationError("actionKey", ValidationErrorCodes.ACTION_NOT_ENABLED,
                            "Action '" + action.actionKey() + "' is not enabled for this row")));
        }

        ActionExecutionContext context = new ActionExecutionContext(reportCode, action, parameters, current.get(),
                callerContext, idempotencyKey);
        RowActionOutcome outcome = handler.apply(context, rowChange);

        return switch (outcome) {
            case RowActionOutcome.Success success -> {
                Map<String, Object> updated = new LinkedHashMap<>(current.get());
                updated.putAll(success.updatedFields());
                rowStore.save(reportCode, rowChange.rowKey(), updated);
                String newVersion = RowIdentity.computeRowVersion(editableColumns, updated);
                yield new RowActionResult(rowChange.rowKey(), RowActionStatus.SUCCESS, newVersion, List.of());
            }
            case RowActionOutcome.Failure failure ->
                    new RowActionResult(rowChange.rowKey(), RowActionStatus.FAILED, currentVersion, failure.errors());
        };
    }
}
