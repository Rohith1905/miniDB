package com.minidb.query.executor;

import com.minidb.query.parser.*;
import com.minidb.storage.table.*;
import com.minidb.transaction.*;
import java.util.*;

/**
 * Query Executor - Executes parsed SQL statements
 */
public class Executor {
    private final Map<String, TableSchema> catalog;
    private final Map<String, Table> tables;
    private final TransactionManager txnManager;

    public Executor(TransactionManager txnManager) {
        this.catalog = new HashMap<>();
        this.tables = new HashMap<>();
        this.txnManager = txnManager;
    }

    /**
     * Execute statement and return result
     */
    public ExecutionResult execute(Statement statement, Transaction txn) {
        try {
            return switch (statement.getType()) {
                case CREATE_TABLE -> executeCreateTable((CreateTableStatement) statement);
                case INSERT -> executeInsert((InsertStatement) statement, txn);
                case SELECT -> executeSelect((SelectStatement) statement, txn);
                case UPDATE -> executeUpdate((UpdateStatement) statement, txn);
                case DELETE -> executeDelete((DeleteStatement) statement, txn);
            };
        } catch (Exception e) {
            return new ExecutionResult(false, 0, List.of(), e.getMessage());
        }
    }

    private ExecutionResult executeCreateTable(CreateTableStatement stmt) {
        if (catalog.containsKey(stmt.tableName())) {
            return new ExecutionResult(false, 0, List.of(),
                    "Table " + stmt.tableName() + " already exists");
        }

        // Create schema
        TableSchema schema = new TableSchema(
                stmt.tableName(),
                stmt.columns(),
                stmt.primaryKeyColumn());
        catalog.put(stmt.tableName(), schema);

        // Create table
        boolean hasPK = stmt.primaryKeyColumn() != null;
        Table table = new Table(stmt.tableName(), hasPK);
        tables.put(stmt.tableName(), table);

        return new ExecutionResult(true, 0, List.of(),
                "Table " + stmt.tableName() + " created");
    }

    private ExecutionResult executeInsert(InsertStatement stmt, Transaction txn) {
        Table table = tables.get(stmt.tableName());
        if (table == null) {
            return new ExecutionResult(false, 0, List.of(),
                    "Table " + stmt.tableName() + " does not exist");
        }

        TableSchema schema = catalog.get(stmt.tableName());

        // Create record from values
        com.minidb.storage.table.Record record = new com.minidb.storage.table.Record();
        for (int i = 0; i < stmt.values().size() && i < schema.columns().size(); i++) {
            Object value = stmt.values().get(i);
            var colDef = schema.columns().get(i);

            switch (colDef.type()) {
                case INT -> record.addField((Integer) value);
                case LONG -> record.addField(((Integer) value).longValue());
                case VARCHAR -> record.addField((String) value);
            }
        }

        int recordId = table.insertRecord(record);

        return new ExecutionResult(true, 1, List.of(),
                "Inserted 1 row with ID " + recordId);
    }

    private ExecutionResult executeSelect(SelectStatement stmt, Transaction txn) {
        Table table = tables.get(stmt.tableName());
        if (table == null) {
            return new ExecutionResult(false, 0, List.of(),
                    "Table " + stmt.tableName() + " does not exist");
        }

        List<com.minidb.storage.table.Record> results;

        if (stmt.whereClause() == null) {
            // Full table scan
            results = table.fullScan();
        } else if (stmt.whereClause() instanceof SimpleCondition simple) {
            // Try to use index if primary key
            TableSchema schema = catalog.get(stmt.tableName());
            if (schema.primaryKeyColumn() != null &&
                    simple.column().equals(schema.primaryKeyColumn()) &&
                    simple.operator() == SimpleCondition.Operator.EQ) {

                // Index lookup
                int key = (Integer) simple.value();
                com.minidb.storage.table.Record record = table.searchByPrimaryKey(key);
                results = record != null ? List.of(record) : List.of();
            } else {
                // Scan with filter
                results = scanWithFilter(table, stmt.whereClause());
            }
        } else if (stmt.whereClause() instanceof BetweenCondition between) {
            // Range scan if primary key
            TableSchema schema = catalog.get(stmt.tableName());
            if (schema.primaryKeyColumn() != null &&
                    between.column().equals(schema.primaryKeyColumn())) {

                int start = (Integer) between.startValue();
                int end = (Integer) between.endValue();
                results = table.rangeScanByPrimaryKey(start, end);
            } else {
                results = scanWithFilter(table, stmt.whereClause());
            }
        } else {
            results = scanWithFilter(table, stmt.whereClause());
        }

        return new ExecutionResult(true, results.size(), results,
                "Retrieved " + results.size() + " rows");
    }

    private ExecutionResult executeUpdate(UpdateStatement stmt, Transaction txn) {
        Table table = tables.get(stmt.tableName());
        if (table == null) {
            return new ExecutionResult(false, 0, List.of(),
                    "Table " + stmt.tableName() + " does not exist");
        }

        // Find records to update
        List<com.minidb.storage.table.Record> toUpdate = stmt.whereClause() == null ? table.fullScan()
                : scanWithFilter(table, stmt.whereClause());

        int updated = 0;
        for (com.minidb.storage.table.Record record : toUpdate) {
            // Create updated record
            com.minidb.storage.table.Record newRecord = updateRecord(record, stmt.assignments(),
                    catalog.get(stmt.tableName()));
            if (table.updateRecord(0, newRecord)) { // Simplified
                updated++;
            }
        }

        return new ExecutionResult(true, updated, List.of(),
                "Updated " + updated + " rows");
    }

    private ExecutionResult executeDelete(DeleteStatement stmt, Transaction txn) {
        Table table = tables.get(stmt.tableName());
        if (table == null) {
            return new ExecutionResult(false, 0, List.of(),
                    "Table " + stmt.tableName() + " does not exist");
        }

        List<com.minidb.storage.table.Record> toDelete = stmt.whereClause() == null ? table.fullScan()
                : scanWithFilter(table, stmt.whereClause());

        int deleted = 0;
        for (com.minidb.storage.table.Record record : toDelete) {
            if (table.deleteRecord(0)) { // Simplified
                deleted++;
            }
        }

        return new ExecutionResult(true, deleted, List.of(),
                "Deleted " + deleted + " rows");
    }

    private List<com.minidb.storage.table.Record> scanWithFilter(Table table, Condition condition) {
        List<com.minidb.storage.table.Record> results = new ArrayList<>();

        for (com.minidb.storage.table.Record record : table.fullScan()) {
            if (evaluateCondition(record, condition)) {
                results.add(record);
            }
        }

        return results;
    }

    private boolean evaluateCondition(com.minidb.storage.table.Record record, Condition condition) {
        if (condition instanceof SimpleCondition simple) {
            // Simplified: assume first field is what we're comparing
            Object recordValue = record.getFieldAsInt(0);
            Object condValue = simple.value();

            return switch (simple.operator()) {
                case EQ -> recordValue.equals(condValue);
                case NEQ -> !recordValue.equals(condValue);
                case LT -> ((Integer) recordValue) < (Integer) condValue;
                case GT -> ((Integer) recordValue) > (Integer) condValue;
                case LTE -> ((Integer) recordValue) <= (Integer) condValue;
                case GTE -> ((Integer) recordValue) >= (Integer) condValue;
            };
        } else if (condition instanceof BetweenCondition between) {
            int recordValue = record.getFieldAsInt(0);
            int start = (Integer) between.startValue();
            int end = (Integer) between.endValue();
            return recordValue >= start && recordValue <= end;
        }

        return true;
    }

    private com.minidb.storage.table.Record updateRecord(com.minidb.storage.table.Record oldRecord,
            Map<String, Object> assignments,
            TableSchema schema) {
        com.minidb.storage.table.Record newRecord = new com.minidb.storage.table.Record();

        for (int i = 0; i < schema.columns().size(); i++) {
            var colDef = schema.columns().get(i);

            if (assignments.containsKey(colDef.name())) {
                Object newValue = assignments.get(colDef.name());
                switch (colDef.type()) {
                    case INT -> newRecord.addField((Integer) newValue);
                    case LONG -> newRecord.addField(((Integer) newValue).longValue());
                    case VARCHAR -> newRecord.addField((String) newValue);
                }
            } else {
                // Keep old value
                newRecord.addField(oldRecord.getField(i));
            }
        }

        return newRecord;
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    public TableSchema getSchema(String name) {
        return catalog.get(name);
    }
}
