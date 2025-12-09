package com.minidb.query.parser;

import java.util.List;

/**
 * INSERT statement
 */
public record InsertStatement(
        String tableName,
        List<String> columns,
        List<Object> values) implements Statement {
    @Override
    public StatementType getType() {
        return StatementType.INSERT;
    }
}
