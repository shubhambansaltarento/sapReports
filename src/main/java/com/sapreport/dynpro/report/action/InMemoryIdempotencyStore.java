package com.sapreport.dynpro.report.action;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Placeholder {@link IdempotencyStore} — in-memory, single-instance, no retention-window eviction yet, lost on restart. */
@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, ActionResponse> responsesByKey = new ConcurrentHashMap<>();

    @Override
    public Optional<ActionResponse> find(String reportCode, String actionKey, String idempotencyKey) {
        return Optional.ofNullable(responsesByKey.get(key(reportCode, actionKey, idempotencyKey)));
    }

    @Override
    public void save(String reportCode, String actionKey, String idempotencyKey, ActionResponse response) {
        responsesByKey.put(key(reportCode, actionKey, idempotencyKey), response);
    }

    private String key(String reportCode, String actionKey, String idempotencyKey) {
        return reportCode + "|" + actionKey + "|" + idempotencyKey;
    }
}
