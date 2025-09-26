package com.minidb.transaction.wal;

/**
 * Log record types
 */
public enum LogRecordType {
    BEGIN, UPDATE, COMMIT, ABORT
}