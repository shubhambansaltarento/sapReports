package com.sapreport.dynpro.report.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Both WARRANTY_LABOUR_* reports are separate report codes sharing one
 * {@code reportGroup} (rendered as tabs) — not one report with a mode flag —
 * and WARRANTY_LABOUR_REPORT is deliberately scaffolded with no columns yet.
 */
class WarrantyLabourChargeMetadataTest {

    private ClasspathReportMetadataRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        repository = new ClasspathReportMetadataRepository();
        repository.loadMetadata();
    }

    @Test
    void orderDetailsAndReport_areSeparateReportCodesInTheSameGroup() {
        ReportMetadata orderDetails = repository.findByReportCode("WARRANTY_LABOUR_ORDER_DETAILS").orElseThrow();
        ReportMetadata report = repository.findByReportCode("WARRANTY_LABOUR_REPORT").orElseThrow();

        assertThat(orderDetails.reportGroup()).isEqualTo("WARRANTY_LABOUR_CHARGE");
        assertThat(report.reportGroup()).isEqualTo("WARRANTY_LABOUR_CHARGE");
        assertThat(orderDetails.groupLabel()).isEqualTo(report.groupLabel()).isEqualTo("Warranty Labour Charge");
        assertThat(orderDetails.groupOrder()).isNotEqualTo(report.groupOrder());
    }

    @Test
    void orderDetails_hasRowKeyAndFourActions() {
        ReportMetadata metadata = repository.findByReportCode("WARRANTY_LABOUR_ORDER_DETAILS").orElseThrow();

        assertThat(metadata.rowKey().fields()).containsExactly("orderNumber", "referenceCreditMemoNo");
        assertThat(metadata.actions()).extracting(ActionDefinition::actionKey)
                .containsExactlyInAnyOrder("saveOrderDetails", "downloadInvoicePdf", "initiateESign", "pushESignToTvsm");

        ActionDefinition download = metadata.actions().stream()
                .filter(a -> a.actionKey().equals("downloadInvoicePdf")).findFirst().orElseThrow();
        assertThat(download.returns()).isEqualTo("DOCUMENT");
    }

    @Test
    void report_isScaffoldedWithLookupParameterAndNoColumnsOrActionsYet() {
        ReportMetadata metadata = repository.findByReportCode("WARRANTY_LABOUR_REPORT").orElseThrow();

        assertThat(metadata.columnGroups()).isEmpty();
        assertThat(metadata.actions()).isEmpty();

        ParameterDefinition salesOrg = metadata.parameters().stream()
                .filter(p -> p.name().equals("salesOrganisation")).findFirst().orElseThrow();
        assertThat(salesOrg.control()).isEqualTo(ControlType.LOOKUP);
        assertThat(salesOrg.required()).isTrue();
        assertThat(salesOrg.lookup().endpoint()).isEqualTo("/api/v1/reports/WARRANTY_LABOUR_REPORT/lookups/salesOrganisation");

        assertThat(metadata.dateRangeConstraints()).extracting(DateRangeConstraint::maxRangeDays).containsExactly(366);
    }
}
