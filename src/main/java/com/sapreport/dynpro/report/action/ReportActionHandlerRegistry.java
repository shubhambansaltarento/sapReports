package com.sapreport.dynpro.report.action;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Resolves the {@link ReportActionHandler} for a given report+action — one registry, no per-report if/else in callers. */
@Component
public class ReportActionHandlerRegistry {

    private final List<ReportActionHandler> handlers;

    public ReportActionHandlerRegistry(List<ReportActionHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public Optional<ReportActionHandler> resolve(String reportCode, String actionKey) {
        return handlers.stream()
                .filter(handler -> handler.supports(reportCode, actionKey))
                .findFirst();
    }
}
