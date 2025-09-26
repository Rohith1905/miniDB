package com.minidb.transaction.wal;

import java.nio.ByteBuffer;

public record CommitLogRecord(long lsn, long txnId) implements LogRecord {
    @Override public LogRecordType type() { return LogRecordType.COMMIT; }
    
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.put((byte) 2); // COMMIT type
        buf.putLong(lsn);
        buf.putLong(txnId);
        return buf.array();
    }
}