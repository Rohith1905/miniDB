package com.minidb.query.executor;

import com.minidb.query.parser.CreateTableStatement;
import java.util.List;

/**
 * Table schema metadata
 */
public record TableSchema(
        String name,
        List<CreateTableStatement.ColumnDefinition> columns,
        String primaryKeyColumn) {
}
