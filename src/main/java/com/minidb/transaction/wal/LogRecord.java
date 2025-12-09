package com.minidb.transaction.wal;

/**
 * Base log record
 */
public sealed interface LogRecord permits BeginLogRecord, UpdateLogRecord, 
                                    CommitLogRecord, AbortLogRecord {
    long lsn();
    long txnId();
    LogRecordType type();
    byte[] serialize();
}