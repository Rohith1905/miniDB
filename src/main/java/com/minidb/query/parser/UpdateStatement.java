package com.minidb.query.parser;

import java.util.Map;

/**
 * UPDATE statement
 */
public record UpdateStatement(
        String tableName,
        Map<String, Object> assignments,
        Condition whereClause) implements Statement {
    @Override
    public StatementType getType() {
        return StatementType.UPDATE;
    }
}
