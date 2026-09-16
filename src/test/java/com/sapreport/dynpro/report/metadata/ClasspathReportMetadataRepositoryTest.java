package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.condition.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathReportMetadataRepositoryTest {

    private ClasspathReportMetadataRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        repository = new ClasspathReportMetadataRepository();
        repository.loadMetadata();
    }

    @Test
    void loadsDealerLedgerMetadata() {
        ReportMetadata metadata = repository.findByReportCode("DEALER_LEDGER").orElseThrow();

        assertThat(metadata.title()).isEqualTo("Dealer Ledger");
        assertThat(metadata.configVersion()).isEqualTo("2026.09.2");
        assertThat(metadata.parameters()).extracting(ParameterDefinition::name)
                .containsExactly("dealerCode", "companyCode", "postingDate", "withCblDetails");
        assertThat(metadata.columnGroups()).extracting(ColumnGroup::key).containsExactly("base", "cbl");

        ColumnGroup cblGroup = metadata.columnGroups().get(1);
        assertThat(cblGroup.visibleWhen()).isEqualTo(new Condition.Eq("withCblDetails", true));
    }

    @Test
    void unknownReportCode_isEmpty() {
        assertThat(repository.findByReportCode("NOPE")).isEmpty();
    }
}
