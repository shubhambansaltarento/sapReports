package com.sapreport.dynpro.report.validation;

import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.metadata.ControlType;
import com.sapreport.dynpro.report.metadata.OptionItem;
import com.sapreport.dynpro.report.metadata.ParameterDataType;
import com.sapreport.dynpro.report.metadata.ParameterDefinition;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ValidationRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ReportParameterValidatorTest {

    private final ReportParameterValidator validator = new ReportParameterValidator(new ReportLookupProviderRegistry(List.of()));

    private ReportMetadata metadataWith(ParameterDefinition... parameters) {
        return new ReportMetadata("TEST_REPORT", "Test Report", "v1", List.of(parameters), List.of(), null, null);
    }

    @Test
    void happyPath_coercesEveryControlType() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, true, "TVSL", List.of(new OptionItem("TVSL", "TVS Lucas")), null, null);
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules("2020-04-01", "$TODAY", 366, true), null);
        ParameterDefinition withCbl = new ParameterDefinition("withCblDetails", "With CBL Details",
                ControlType.CHECKBOX, ParameterDataType.BOOLEAN, false, false, null, null, null);
        ReportMetadata metadata = metadataWith(companyCode, postingDate, withCbl);

        Map<String, Object> raw = new HashMap<>();
        raw.put("companyCode", "TVSL");
        raw.put("postingDate", Map.of("from", "2026-04-01", "to", "2026-08-13"));
        raw.put("withCblDetails", true);

        Map<String, Object> result = validator.validateAndCoerce(metadata, raw);

        assertThat(result.get("companyCode")).isEqualTo("TVSL");
        assertThat(result.get("withCblDetails")).isEqualTo(Boolean.TRUE);
        assertThat(result.get("postingDate")).isEqualTo(
                new DateRangeValue(LocalDate.parse("2026-04-01"), LocalDate.parse("2026-08-13")));
    }

    @Test
    void required_missingValueFails() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, true, null, List.of(new OptionItem("TVSL", "TVS Lucas")), null, null);
        ReportMetadata metadata = metadataWith(companyCode);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of()), ValidationException.class);

        assertThat(ex.errors()).hasSize(1);
        assertThat(ex.errors().get(0).field()).isEqualTo("companyCode");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.REQUIRED);
    }

    @Test
    void required_missingDefaultsWhenNotRequired() {
        ParameterDefinition withCbl = new ParameterDefinition("withCblDetails", "With CBL Details",
                ControlType.CHECKBOX, ParameterDataType.BOOLEAN, false, false, null, null, null);
        ReportMetadata metadata = metadataWith(withCbl);

        Map<String, Object> result = validator.validateAndCoerce(metadata, Map.of());

        assertThat(result.get("withCblDetails")).isEqualTo(false);
    }

    @Test
    void typeCoercion_failureIsAValidationErrorNotAnException() {
        ParameterDefinition amount = new ParameterDefinition("amount", "Amount", ControlType.NUMBER,
                ParameterDataType.DECIMAL, true, null, null, null, null);
        ReportMetadata metadata = metadataWith(amount);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("amount", "not-a-number")),
                ValidationException.class);

        assertThat(ex.errors()).hasSize(1);
        assertThat(ex.errors().get(0).field()).isEqualTo("amount");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.TYPE_MISMATCH);
    }

    @Test
    void select_valueNotInOptionsFails() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, true, null, List.of(new OptionItem("TVSL", "TVS Lucas")), null, null);
        ReportMetadata metadata = metadataWith(companyCode);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("companyCode", "BOGUS")),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_OPTION);
    }

    @Test
    void unknownParameter_isRejectedNotSilentlyIgnored() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, false, null, List.of(new OptionItem("TVSL", "TVS Lucas")), null, null);
        ReportMetadata metadata = metadataWith(companyCode);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("bogusParam", "x")),
                ValidationException.class);

        assertThat(ex.errors()).hasSize(1);
        assertThat(ex.errors().get(0).field()).isEqualTo("bogusParam");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.UNKNOWN_PARAMETER);
    }

    @Test
    void dateRange_requireBothBoundsFailsWhenOneMissing() {
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules(null, null, null, true), null);
        ReportMetadata metadata = metadataWith(postingDate);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("postingDate", mapOf("from", "2026-04-01"))),
                ValidationException.class);

        assertThat(ex.errors()).extracting(ValidationError::field).containsExactly("postingDate.to");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.MISSING_BOUND);
    }

    @Test
    void dateRange_minDateViolationFails() {
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules("2020-04-01", null, null, false), null);
        ReportMetadata metadata = metadataWith(postingDate);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata,
                        Map.of("postingDate", mapOf("from", "2019-01-01", "to", "2019-02-01"))),
                ValidationException.class);

        assertThat(ex.errors()).extracting(ValidationError::field).containsExactly("postingDate.from");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.MIN_DATE);
    }

    @Test
    void dateRange_maxDateViolationFails() {
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules(null, "2026-08-13", null, false), null);
        ReportMetadata metadata = metadataWith(postingDate);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata,
                        Map.of("postingDate", mapOf("from", "2026-08-01", "to", "2026-09-01"))),
                ValidationException.class);

        assertThat(ex.errors()).extracting(ValidationError::field).containsExactly("postingDate.to");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.MAX_DATE);
    }

    @Test
    void dateRange_fromAfterToFails() {
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules(null, null, null, false), null);
        ReportMetadata metadata = metadataWith(postingDate);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata,
                        Map.of("postingDate", mapOf("from", "2026-08-13", "to", "2026-01-01"))),
                ValidationException.class);

        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.INVALID_RANGE);
    }

    @Test
    void dateRange_maxRangeExceededFails() {
        ParameterDefinition postingDate = new ParameterDefinition("postingDate", "Date", ControlType.DATE_RANGE,
                null, true, null, null,
                new ValidationRules(null, null, 30, false), null);
        ReportMetadata metadata = metadataWith(postingDate);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata,
                        Map.of("postingDate", mapOf("from", "2026-01-01", "to", "2026-04-01"))),
                ValidationException.class);

        assertThat(ex.errors()).extracting(ValidationError::field).containsExactly("postingDate.to");
        assertThat(ex.errors().get(0).code()).isEqualTo(ValidationErrorCodes.MAX_RANGE_EXCEEDED);
        assertThat(ex.errors().get(0).params()).containsEntry("maxRangeDays", 30);
    }

    @Test
    void multipleErrors_areAllCollectedNotFailFast() {
        ParameterDefinition companyCode = new ParameterDefinition("companyCode", "Company Code", ControlType.SELECT,
                ParameterDataType.STRING, true, null, List.of(new OptionItem("TVSL", "TVS Lucas")), null, null);
        ParameterDefinition amount = new ParameterDefinition("amount", "Amount", ControlType.NUMBER,
                ParameterDataType.DECIMAL, true, null, null, null, null);
        ReportMetadata metadata = metadataWith(companyCode, amount);

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("amount", "abc", "extra", "x")),
                ValidationException.class);

        assertThat(ex.errors()).extracting(ValidationError::code)
                .containsExactlyInAnyOrder(ValidationErrorCodes.UNKNOWN_PARAMETER,
                        ValidationErrorCodes.REQUIRED, ValidationErrorCodes.TYPE_MISMATCH);
    }

    @Test
    void parameterGroupConstraintViolation_attachesErrorToGroupKey() {
        ParameterDefinition divisionVehicle = new ParameterDefinition("divisionVehicle", "Vehicle", ControlType.CHECKBOX,
                ParameterDataType.BOOLEAN, false, false, null, null, null);
        ParameterDefinition divisionSpares = new ParameterDefinition("divisionSpares", "Spares", ControlType.CHECKBOX,
                ParameterDataType.BOOLEAN, false, false, null, null, null);
        com.sapreport.dynpro.report.metadata.ParameterGroup division = new com.sapreport.dynpro.report.metadata.ParameterGroup(
                "division", "Select Division", "Select Only one Check Box at a Time",
                List.of("divisionVehicle", "divisionSpares"),
                com.sapreport.dynpro.report.metadata.ParameterGroupConstraint.EXACTLY_ONE, "EXACTLY_ONE_REQUIRED");
        ReportMetadata metadata = new ReportMetadata("TEST_REPORT", "Test Report", "v1",
                List.of(divisionVehicle, divisionSpares), List.of(), null, null,
                null, null, null, List.of(division), null, List.of());

        ValidationException ex = catchThrowableOfType(
                () -> validator.validateAndCoerce(metadata, Map.of("divisionVehicle", true, "divisionSpares", true)),
                ValidationException.class);

        assertThat(ex.errors()).hasSize(1);
        assertThat(ex.errors().get(0).field()).isEqualTo("division");
        assertThat(ex.errors().get(0).code()).isEqualTo("EXACTLY_ONE_REQUIRED");

        // Selecting exactly one member satisfies the constraint.
        Map<String, Object> coerced = validator.validateAndCoerce(metadata,
                Map.of("divisionVehicle", true, "divisionSpares", false));
        assertThat(coerced).containsEntry("divisionVehicle", true).containsEntry("divisionSpares", false);
    }

    private static Map<String, Object> mapOf(String k1, Object v1) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = mapOf(k1, v1);
        map.put(k2, v2);
        return map;
    }
}
