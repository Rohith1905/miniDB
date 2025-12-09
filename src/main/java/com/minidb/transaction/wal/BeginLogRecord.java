package com.minidb.transaction.wal;

import java.nio.ByteBuffer;

public record BeginLogRecord(long lsn, long txnId) implements LogRecord {
    @Override public LogRecordType type() { return LogRecordType.BEGIN; }
    
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.put((byte) 0); // BEGIN type
        buf.putLong(lsn);
        buf.putLong(txnId);
        return buf.array();
    }
}