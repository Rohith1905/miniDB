package com.minidb.transaction.wal;

import java.nio.ByteBuffer;

public record AbortLogRecord(long lsn, long txnId) implements LogRecord {
    @Override public LogRecordType type() { return LogRecordType.ABORT; }
    
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.put((byte) 3); // ABORT type
        buf.putLong(lsn);
        buf.putLong(txnId);
        return buf.array();
    }
}