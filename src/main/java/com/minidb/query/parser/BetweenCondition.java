package com.minidb.query.parser;

/**
 * BETWEEN condition
 */
public record BetweenCondition(
        String column,
        Object startValue,
        Object endValue) implements Condition {
}
