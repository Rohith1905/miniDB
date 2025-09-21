package com.minidb.query.parser;

/**
 * Compound condition (AND/OR)
 */
public record CompoundCondition(
        Condition left,
        LogicalOperator operator,
        Condition right) implements Condition {
    public enum LogicalOperator {
        AND, OR
    }
}
