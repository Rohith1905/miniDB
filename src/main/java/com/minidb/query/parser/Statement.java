package com.minidb.query.parser;

import java.util.*;

/**
 * Base class for SQL statements
 */
public sealed interface Statement permits
        CreateTableStatement, InsertStatement, SelectStatement,
        UpdateStatement, DeleteStatement {

    StatementType getType();

    enum StatementType {
        CREATE_TABLE, INSERT, SELECT, UPDATE, DELETE
    }
}
