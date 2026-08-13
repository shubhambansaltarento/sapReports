package com.sapreport.dynpro.report.metadata;

import com.sapreport.dynpro.report.condition.Condition;

/** The row action a {@link ColumnType#ACTION} column triggers, and when it's enabled for a given row. */
public record ColumnAction(String actionKey, String icon, Condition enabledWhen) {
}
