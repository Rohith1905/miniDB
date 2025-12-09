package com.minidb.query.executor;

import java.util.List;

/**
 * Execution result
 */
public record ExecutionResult(
        boolean success,
        int rowsAffected,
        List<com.minidb.storage.table.Record> resultSet,
        String message) {
    public void print() {
        System.out.println(message);

        if (!resultSet.isEmpty()) {
            System.out.println("\nResults:");
            for (int i = 0; i < resultSet.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + resultSet.get(i));
            }
        }
    }
}
