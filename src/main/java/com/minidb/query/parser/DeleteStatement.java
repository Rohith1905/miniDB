package com.minidb.query.parser;

/**
 * DELETE statement
 */
public record DeleteStatement(
        String tableName,
        Condition whereClause) implements Statement {
    @Override
    public StatementType getType() {
        return StatementType.DELETE;
    }
}
