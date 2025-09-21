package com.minidb.query.parser;

/**
 * WHERE clause condition
 */
public sealed interface Condition permits SimpleCondition, BetweenCondition, CompoundCondition {
}
