package com.sapreport.dynpro.report.action;

import java.util.Optional;

/**
 * Stores the result of a side-effecting action per (reportCode, actionKey,
 * idempotencyKey) — a replay within the retention window returns the
 * ORIGINAL result rather than re-executing the side effect. Keyed by all
 * three, not the idempotency key alone, so two different actions can't
 * collide on a coincidentally-reused key.
 */
public interface IdempotencyStore {

    Optional<ActionResponse> find(String reportCode, String actionKey, String idempotencyKey);

    void save(String reportCode, String actionKey, String idempotencyKey, ActionResponse response);
}
