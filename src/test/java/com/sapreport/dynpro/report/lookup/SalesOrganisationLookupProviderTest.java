package com.sapreport.dynpro.report.lookup;

import com.sapreport.dynpro.report.security.ReportCallerContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The lookup result set must never include a sales org outside the caller's authorized scope, query or no query. */
class SalesOrganisationLookupProviderTest {

    private ReportCallerContext callerWithScope(Set<String> authorizedSalesOrganisations) {
        return new ReportCallerContext() {
            @Override
            public String dealerCode() {
                return "1130";
            }

            @Override
            public String dealerDescription() {
                return "PAWAN SARKAR AUTOMOBILES";
            }

            @Override
            public Set<String> authorizedCompanyCodes() {
                return Set.of("TVSL");
            }

            @Override
            public Set<String> authorizedSalesOrganisations() {
                return authorizedSalesOrganisations;
            }
        };
    }

    @Test
    void search_onlyReturnsOrgsWithinCallerScope() {
        SalesOrganisationLookupProvider provider = new SalesOrganisationLookupProvider(
                callerWithScope(Set.of("TVSL01")));

        LookupResponse response = provider.search("WARRANTY_LABOUR_REPORT", "salesOrganisation", null, 1, 25);

        assertThat(response.items()).extracting(LookupItem::value).containsExactly("TVSL01");
    }

    @Test
    void search_withNoAuthorizedOrgs_returnsEmpty() {
        SalesOrganisationLookupProvider provider = new SalesOrganisationLookupProvider(callerWithScope(Set.of()));

        LookupResponse response = provider.search("WARRANTY_LABOUR_REPORT", "salesOrganisation", null, 1, 25);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalItems()).isZero();
    }

    @Test
    void search_queryText_isAppliedWithinScope_notAcrossIt() {
        SalesOrganisationLookupProvider provider = new SalesOrganisationLookupProvider(
                callerWithScope(Set.of("TVSL01", "TVSL02", "TVSM01")));

        // "TVS" matches all three by label/value, but a query for "Motor" should still only surface the
        // in-scope org that matches, never reach outside scope regardless of how permissive the query is.
        LookupResponse motorMatches = provider.search("WARRANTY_LABOUR_REPORT", "salesOrganisation", "Motor", 1, 25);
        assertThat(motorMatches.items()).extracting(LookupItem::value).containsExactly("TVSM01");

        SalesOrganisationLookupProvider scopedOutProvider = new SalesOrganisationLookupProvider(
                callerWithScope(Set.of("TVSL01")));
        LookupResponse scopedOut = scopedOutProvider.search("WARRANTY_LABOUR_REPORT", "salesOrganisation", "Motor", 1, 25);
        assertThat(scopedOut.items()).isEmpty();
    }

    @Test
    void supports_onlyMatchesItsOwnReportAndParameter() {
        SalesOrganisationLookupProvider provider = new SalesOrganisationLookupProvider(callerWithScope(Set.of("TVSL01")));

        assertThat(provider.supports("WARRANTY_LABOUR_REPORT", "salesOrganisation")).isTrue();
        assertThat(provider.supports("WARRANTY_LABOUR_REPORT", "otherParam")).isFalse();
        assertThat(provider.supports("OTHER_REPORT", "salesOrganisation")).isFalse();
    }
}
