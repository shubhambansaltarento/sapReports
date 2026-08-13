package com.sapreport.dynpro.report.metadata;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads every {@code classpath:reports/*.json} file at startup into memory.
 * One file per report; see {@code reports-overview.md} for the metadata
 * shape and {@code src/main/resources/reports/dealer-ledger.json} for the
 * reference example.
 */
@Repository
public class ClasspathReportMetadataRepository implements ReportMetadataRepository {

    private static final String LOCATION_PATTERN = "classpath:reports/*.json";

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();
    private final Map<String, ReportMetadata> metadataByReportCode = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadMetadata() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(LOCATION_PATTERN);
        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                ReportMetadata metadata = objectMapper.readValue(in, ReportMetadata.class);
                metadataByReportCode.put(metadata.reportCode(), metadata);
            }
        }
    }

    @Override
    public Optional<ReportMetadata> findByReportCode(String reportCode) {
        return Optional.ofNullable(metadataByReportCode.get(reportCode));
    }
}
