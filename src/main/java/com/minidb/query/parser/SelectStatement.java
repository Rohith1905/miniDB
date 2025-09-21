package com.minidb.query.parser;

import java.util.List;

/**
 * SELECT statement
 */
public record SelectStatement(
        List<String> columns,
        String tableName,
        Condition whereClause) implements Statement {
    @Override
    public StatementType getType() {
        return StatementType.SELECT;
    }

    public boolean isSelectAll() {
        return columns.size() == 1 && columns.get(0).equals("*");
    }
}
