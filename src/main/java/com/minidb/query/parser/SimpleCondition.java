package com.minidb.query.parser;

/**
 * Simple condition (column operator value)
 */
public record SimpleCondition(
        String column,
        Operator operator,
        Object value) implements Condition {
    public enum Operator {
        EQ, NEQ, LT, GT, LTE, GTE
    }
}
