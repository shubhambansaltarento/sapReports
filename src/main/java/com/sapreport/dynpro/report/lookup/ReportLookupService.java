package com.sapreport.dynpro.report.lookup;

import com.sapreport.dynpro.report.exception.ReportNotFoundException;
import com.sapreport.dynpro.report.metadata.ControlType;
import com.sapreport.dynpro.report.metadata.ParameterDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ReportMetadataRepository;
import com.sapreport.dynpro.report.validation.ValidationError;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import com.sapreport.dynpro.report.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Backs {@code GET /api/v1/reports/{reportCode}/lookups/{parameterName}}. */
@Service
public class ReportLookupService {

    private final ReportMetadataRepository metadataRepository;
    private final ReportLookupProviderRegistry providerRegistry;

    public ReportLookupService(ReportMetadataRepository metadataRepository, ReportLookupProviderRegistry providerRegistry) {
        this.metadataRepository = metadataRepository;
        this.providerRegistry = providerRegistry;
    }

    public LookupResponse search(String reportCode, String parameterName, String query, Integer page, Integer pageSize) {
        ReportMetadata metadata = metadataRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new ReportNotFoundException(reportCode));

        ParameterDefinition parameter = metadata.parameters().stream()
                .filter(p -> p.name().equals(parameterName))
                .findFirst()
                .orElseThrow(() -> new ValidationException(List.of(new ValidationError("parameterName",
                        ValidationErrorCodes.UNKNOWN_PARAMETER,
                        "'" + parameterName + "' is not a parameter of report '" + reportCode + "'"))));

        if (parameter.control() != ControlType.LOOKUP) {
            throw new ValidationException(List.of(new ValidationError("parameterName", ValidationErrorCodes.TYPE_MISMATCH,
                    "'" + parameterName + "' is not a LOOKUP parameter")));
        }

        ReportLookupProvider provider = providerRegistry.find(reportCode, parameterName)
                .orElseThrow(() -> new IllegalStateException(
                        "No ReportLookupProvider registered for " + reportCode + "/" + parameterName));

        int effectivePage = page != null && page > 0 ? page : 1;
        int effectivePageSize = pageSize != null && pageSize > 0
                ? pageSize
                : parameter.lookup() != null && parameter.lookup().pageSize() != null ? parameter.lookup().pageSize() : 25;

        return provider.search(reportCode, parameterName, query, effectivePage, effectivePageSize);
    }
}
