package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.validation.ReportParameterValidator;
import com.sapreport.dynpro.report.validation.ValidationErrorCodes;
import com.sapreport.dynpro.report.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Validates the DEALER_SHIPMENT_CLOSURE metadata itself (loaded from
 * classpath JSON, not hand-built) against the generic
 * {@link ReportParameterValidator}: the division checkbox-group's
 * EXACTLY_ONE constraint and its resolution into a single internal
 * {@code division} value, plus the dateFrom/dateTo cross-field max-range
 * check.
 */
class DealerShipmentClosureMetadataTest {

    private ReportMetadata metadata;
    private ReportParameterValidator validator;

    @BeforeEach
    void setUp() throws IOException {
        ClasspathReportMetadataRepository repository = new ClasspathReportMetadataRepository();
        repository.loadMetadata();
        metadata = repository.findByReportCode("DEALER_SHIPMENT_CLOSURE").orElseThrow();
        validator = new ReportParameterValidator(new ReportLookupProviderRegistry(List.of()));
    }

    private Map<String, Object> baseParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("dateFrom", "2026-08-01");
        params.put("dateTo", "2026-08-10");
        return params;
    }

    @Test
    void exactlyOneViolation_zeroSelected() {
        Map<String, Object> params = baseParameters();
        params.put("divisionVehicle", false);
        params.put("divisionSpares", false);

        ValidationException ex = catchThrowableOfType(() -> validator.validateAndCoerce(metadata, params),
                ValidationException.class);

        assertThat(ex.errors()).extracting(e -> e.field()).contains("division");
        assertThat(ex.errors()).filteredOn(e -> e.field().equals("division"))
                .extracting(e -> e.code()).containsExactly("EXACTLY_ONE_REQUIRED");
    }

    @Test
    void exactlyOneViolation_bothSelected() {
        Map<String, Object> params = baseParameters();
        params.put("divisionVehicle", true);
        params.put("divisionSpares", true);

        ValidationException ex = catchThrowableOfType(() -> validator.validateAndCoerce(metadata, params),
                ValidationException.class);

        assertThat(ex.errors()).filteredOn(e -> e.field().equals("division"))
                .extracting(e -> e.code()).containsExactly("EXACTLY_ONE_REQUIRED");
    }

    @Test
    void exactlyOneSatisfied_resolvesDivisionEnum() {
        Map<String, Object> vehicleParams = baseParameters();
        vehicleParams.put("divisionVehicle", true);
        vehicleParams.put("divisionSpares", false);
        assertThat(validator.validateAndCoerce(metadata, vehicleParams)).containsEntry("division", "VEHICLE");

        Map<String, Object> sparesParams = baseParameters();
        sparesParams.put("divisionVehicle", false);
        sparesParams.put("divisionSpares", true);
        assertThat(validator.validateAndCoerce(metadata, sparesParams)).containsEntry("division", "SPARES");
    }

    @Test
    void dateRange_exceedingMaxDays_isRejected() {
        Map<String, Object> params = new HashMap<>();
        params.put("dateFrom", "2026-01-01");
        params.put("dateTo", "2026-12-31");
        params.put("divisionVehicle", true);
        params.put("divisionSpares", false);

        ValidationException ex = catchThrowableOfType(() -> validator.validateAndCoerce(metadata, params),
                ValidationException.class);

        assertThat(ex.errors()).extracting(e -> e.field()).contains("dateTo");
        assertThat(ex.errors()).filteredOn(e -> e.field().equals("dateTo"))
                .extracting(e -> e.code()).contains(ValidationErrorCodes.MAX_RANGE_EXCEEDED);
    }
}
