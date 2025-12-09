package com.minidb.query.parser;

import java.util.List;

/**
 * CREATE TABLE statement
 */
public record CreateTableStatement(
        String tableName,
        List<ColumnDefinition> columns,
        String primaryKeyColumn) implements Statement {
    @Override
    public StatementType getType() {
        return StatementType.CREATE_TABLE;
    }

    public record ColumnDefinition(String name, ColumnType type, int length) {
        public enum ColumnType {
            INT, LONG, VARCHAR
        }
    }
}
