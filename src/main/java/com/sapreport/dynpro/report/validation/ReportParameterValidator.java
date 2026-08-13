package com.sapreport.dynpro.report.validation;

import com.sapreport.dynpro.report.lookup.ReportLookupProvider;
import com.sapreport.dynpro.report.lookup.ReportLookupProviderRegistry;
import com.sapreport.dynpro.report.metadata.DateRangeConstraint;
import com.sapreport.dynpro.report.metadata.ParameterDataType;
import com.sapreport.dynpro.report.metadata.ParameterDefinition;
import com.sapreport.dynpro.report.metadata.ParameterGroup;
import com.sapreport.dynpro.report.metadata.ReportMetadata;
import com.sapreport.dynpro.report.metadata.ValidationRules;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and coerces a report's raw request {@code parameters} map
 * against its {@link ReportMetadata}. Collects every failure rather than
 * failing fast, and throws once with the full list (see
 * {@link ValidationException}). This is metadata-driven by design — there
 * is no per-report subclass; the same engine serves all reports.
 */
@Component
public class ReportParameterValidator {

    private final ReportLookupProviderRegistry lookupProviderRegistry;

    public ReportParameterValidator(ReportLookupProviderRegistry lookupProviderRegistry) {
        this.lookupProviderRegistry = lookupProviderRegistry;
    }

    public Map<String, Object> validateAndCoerce(ReportMetadata metadata, Map<String, Object> rawParameters) {
        Map<String, Object> raw = rawParameters == null ? Map.of() : rawParameters;
        List<ValidationError> errors = new ArrayList<>();
        Map<String, Object> coerced = new LinkedHashMap<>();

        Set<String> knownNames = metadata.parameters().stream()
                .map(ParameterDefinition::name)
                .collect(Collectors.toSet());

        for (String key : raw.keySet()) {
            if (!knownNames.contains(key)) {
                errors.add(new ValidationError(key, ValidationErrorCodes.UNKNOWN_PARAMETER,
                        "Unknown parameter '" + key + "'"));
            }
        }

        for (ParameterDefinition def : metadata.parameters()) {
            Object value = raw.get(def.name());
            boolean missing = value == null || (value instanceof String s && s.isBlank());
            if (missing) {
                if (def.required()) {
                    errors.add(new ValidationError(def.name(), ValidationErrorCodes.REQUIRED,
                            def.label() + " is required"));
                } else if (def.defaultValue() != null) {
                    coerced.put(def.name(), def.defaultValue());
                }
                continue;
            }
            coerceAndValidate(metadata, def, value, errors, coerced);
        }

        validateParameterGroups(metadata, coerced, errors);
        validateDateRangeConstraints(metadata, coerced, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return coerced;
    }

    private void validateParameterGroups(ReportMetadata metadata, Map<String, Object> coerced,
                                          List<ValidationError> errors) {
        for (ParameterGroup group : metadata.parameterGroups()) {
            List<String> selectedMembers = group.members().stream()
                    .filter(member -> Boolean.TRUE.equals(coerced.get(member)))
                    .toList();
            boolean violated = switch (group.constraint()) {
                case EXACTLY_ONE -> selectedMembers.size() != 1;
                case AT_MOST_ONE -> selectedMembers.size() > 1;
                case AT_LEAST_ONE -> selectedMembers.isEmpty();
            };
            if (violated) {
                String message = group.hint() != null ? group.hint() : "Constraint violated for group '" + group.key() + "'";
                errors.add(new ValidationError(group.key(), group.errorCode(), message));
                continue;
            }
            if (group.resolvesTo() != null && !selectedMembers.isEmpty()) {
                String resolved = group.resolvesTo().valueByMember().get(selectedMembers.get(0));
                coerced.put(group.resolvesTo().targetField(), resolved);
            }
        }
    }

    private void validateDateRangeConstraints(ReportMetadata metadata, Map<String, Object> coerced,
                                               List<ValidationError> errors) {
        for (DateRangeConstraint constraint : metadata.dateRangeConstraints()) {
            LocalDate from = coerced.get(constraint.fromField()) instanceof LocalDate d ? d : null;
            LocalDate to = coerced.get(constraint.toField()) instanceof LocalDate d ? d : null;

            if (Boolean.TRUE.equals(constraint.requireBothBounds())) {
                if (from == null) {
                    errors.add(new ValidationError(constraint.fromField(), ValidationErrorCodes.MISSING_BOUND,
                            "A start date is required"));
                }
                if (to == null) {
                    errors.add(new ValidationError(constraint.toField(), ValidationErrorCodes.MISSING_BOUND,
                            "An end date is required"));
                }
            }

            if (from != null && to != null) {
                if (from.isAfter(to)) {
                    errors.add(new ValidationError(constraint.toField(), ValidationErrorCodes.INVALID_RANGE,
                            "Start date must not be after end date"));
                } else if (constraint.maxRangeDays() != null) {
                    long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
                    if (rangeDays > constraint.maxRangeDays()) {
                        errors.add(new ValidationError(constraint.toField(), ValidationErrorCodes.MAX_RANGE_EXCEEDED,
                                "Date range cannot exceed " + constraint.maxRangeDays() + " days",
                                Map.of("maxRangeDays", constraint.maxRangeDays())));
                    }
                }
            }
        }
    }

    private void coerceAndValidate(ReportMetadata metadata, ParameterDefinition def, Object raw,
                                    List<ValidationError> errors, Map<String, Object> coerced) {
        switch (def.control()) {
            case CHECKBOX -> coerceBoolean(def, raw, errors, coerced);
            case SELECT -> coerceSelect(def, raw, errors, coerced);
            case MULTI_SELECT -> coerceMultiSelect(def, raw, errors, coerced);
            case DATE_RANGE -> coerceDateRange(def, raw, errors, coerced);
            case DATE -> coerceDate(def, raw, errors, coerced);
            case TEXT, NUMBER -> coerceScalar(def, raw, errors, coerced);
            case LOOKUP -> coerceLookup(metadata, def, raw, errors, coerced);
        }
    }

    private void coerceLookup(ReportMetadata metadata, ParameterDefinition def, Object raw,
                               List<ValidationError> errors, Map<String, Object> coerced) {
        boolean multiple = Boolean.TRUE.equals(def.multiple());
        List<String> values;
        if (multiple) {
            if (!(raw instanceof List<?> rawList)) {
                errors.add(new ValidationError(def.name(), ValidationErrorCodes.TYPE_MISMATCH,
                        def.label() + " must be a list of values"));
                return;
            }
            values = rawList.stream().map(String::valueOf).toList();
        } else {
            values = List.of(String.valueOf(raw));
        }

        Optional<ReportLookupProvider> provider = lookupProviderRegistry.find(metadata.reportCode(), def.name());
        if (provider.isPresent()) {
            boolean allValid = values.stream().allMatch(v -> provider.get().isValidValue(metadata.reportCode(), def.name(), v));
            if (!allValid) {
                errors.add(new ValidationError(def.name(), ValidationErrorCodes.INVALID_OPTION,
                        "One or more values are not valid options for " + def.label()));
                return;
            }
        }
        coerced.put(def.name(), multiple ? values : values.get(0));
    }

    private void coerceBoolean(ParameterDefinition def, Object raw, List<ValidationError> errors,
                                Map<String, Object> coerced) {
        Boolean value = switch (raw) {
            case Boolean b -> b;
            case String s when s.equalsIgnoreCase("true") -> Boolean.TRUE;
            case String s when s.equalsIgnoreCase("false") -> Boolean.FALSE;
            default -> null;
        };
        if (value == null) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.TYPE_MISMATCH,
                    def.label() + " must be a boolean"));
            return;
        }
        coerced.put(def.name(), value);
    }

    private void coerceSelect(ParameterDefinition def, Object raw, List<ValidationError> errors,
                               Map<String, Object> coerced) {
        String value = String.valueOf(raw);
        if (!isKnownOption(def, value)) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.INVALID_OPTION,
                    "'" + value + "' is not a valid option for " + def.label()));
            return;
        }
        coerced.put(def.name(), value);
    }

    private void coerceMultiSelect(ParameterDefinition def, Object raw, List<ValidationError> errors,
                                    Map<String, Object> coerced) {
        if (!(raw instanceof List<?> rawList)) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.TYPE_MISMATCH,
                    def.label() + " must be a list of values"));
            return;
        }
        List<String> values = new ArrayList<>();
        boolean allKnown = true;
        for (Object item : rawList) {
            String value = String.valueOf(item);
            values.add(value);
            allKnown &= isKnownOption(def, value);
        }
        if (!allKnown) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.INVALID_OPTION,
                    "One or more values are not valid options for " + def.label()));
            return;
        }
        coerced.put(def.name(), values);
    }

    private boolean isKnownOption(ParameterDefinition def, String value) {
        return def.options() != null && def.options().stream().anyMatch(o -> o.value().equals(value));
    }

    private void coerceScalar(ParameterDefinition def, Object raw, List<ValidationError> errors,
                               Map<String, Object> coerced) {
        ParameterDataType dataType = def.dataType() != null ? def.dataType() : ParameterDataType.STRING;
        try {
            Object value = switch (dataType) {
                case STRING -> String.valueOf(raw);
                case BOOLEAN -> Boolean.parseBoolean(String.valueOf(raw));
                case INTEGER -> raw instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(raw));
                case DECIMAL -> raw instanceof Number n
                        ? new BigDecimal(n.toString())
                        : new BigDecimal(String.valueOf(raw));
                case DATE -> LocalDate.parse(String.valueOf(raw));
            };
            coerced.put(def.name(), value);
        } catch (RuntimeException e) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.TYPE_MISMATCH,
                    def.label() + " has an invalid value"));
        }
    }

    private void coerceDate(ParameterDefinition def, Object raw, List<ValidationError> errors,
                             Map<String, Object> coerced) {
        LocalDate date = parseOptionalDate(raw, def.name(), errors);
        if (date == null) {
            return;
        }
        if (def.validation() != null) {
            checkMinBound(def.name(), date, def.validation().minDate(), errors);
            checkMaxBound(def.name(), date, def.validation().maxDate(), errors);
        }
        coerced.put(def.name(), date);
    }

    private void coerceDateRange(ParameterDefinition def, Object raw, List<ValidationError> errors,
                                  Map<String, Object> coerced) {
        if (!(raw instanceof Map<?, ?> rawMap)) {
            errors.add(new ValidationError(def.name(), ValidationErrorCodes.TYPE_MISMATCH,
                    def.label() + " must be a {from, to} date range"));
            return;
        }
        String fromField = def.name() + ".from";
        String toField = def.name() + ".to";
        LocalDate from = parseOptionalDate(rawMap.get("from"), fromField, errors);
        LocalDate to = parseOptionalDate(rawMap.get("to"), toField, errors);

        ValidationRules rules = def.validation();
        if (rules != null) {
            if (Boolean.TRUE.equals(rules.requireBothBounds())) {
                if (from == null) {
                    errors.add(new ValidationError(fromField, ValidationErrorCodes.MISSING_BOUND,
                            def.label() + " requires a start date"));
                }
                if (to == null) {
                    errors.add(new ValidationError(toField, ValidationErrorCodes.MISSING_BOUND,
                            def.label() + " requires an end date"));
                }
            }
            if (from != null) {
                checkMinBound(fromField, from, rules.minDate(), errors);
            }
            if (to != null) {
                checkMaxBound(toField, to, rules.maxDate(), errors);
            }
            if (from != null && to != null) {
                if (from.isAfter(to)) {
                    errors.add(new ValidationError(toField, ValidationErrorCodes.INVALID_RANGE,
                            def.label() + " start date must not be after end date"));
                } else if (rules.maxRangeDays() != null) {
                    long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;
                    if (rangeDays > rules.maxRangeDays()) {
                        errors.add(new ValidationError(toField, ValidationErrorCodes.MAX_RANGE_EXCEEDED,
                                "Date range cannot exceed " + rules.maxRangeDays() + " days",
                                Map.of("maxRangeDays", rules.maxRangeDays())));
                    }
                }
            }
        }
        coerced.put(def.name(), new DateRangeValue(from, to));
    }

    private LocalDate parseOptionalDate(Object raw, String field, List<ValidationError> errors) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(raw));
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(field, ValidationErrorCodes.TYPE_MISMATCH,
                    "'" + raw + "' is not a valid date (expected yyyy-MM-dd)"));
            return null;
        }
    }

    private void checkMinBound(String field, LocalDate date, String minDateLiteral, List<ValidationError> errors) {
        if (minDateLiteral == null) {
            return;
        }
        LocalDate bound = resolveDateLiteral(minDateLiteral);
        if (date.isBefore(bound)) {
            errors.add(new ValidationError(field, ValidationErrorCodes.MIN_DATE,
                    "Date must not be before " + bound, Map.of("minDate", bound.toString())));
        }
    }

    private void checkMaxBound(String field, LocalDate date, String maxDateLiteral, List<ValidationError> errors) {
        if (maxDateLiteral == null) {
            return;
        }
        LocalDate bound = resolveDateLiteral(maxDateLiteral);
        if (date.isAfter(bound)) {
            errors.add(new ValidationError(field, ValidationErrorCodes.MAX_DATE,
                    "Date must not be after " + bound, Map.of("maxDate", bound.toString())));
        }
    }

    private LocalDate resolveDateLiteral(String literal) {
        return "$TODAY".equals(literal) ? LocalDate.now() : LocalDate.parse(literal);
    }
}
