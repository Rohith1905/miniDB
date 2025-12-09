package com.minidb.transaction.wal;

import com.minidb.storage.page.PageId;
import java.nio.ByteBuffer;

public record UpdateLogRecord(
    long lsn,
    long txnId,
    PageId pageId,
    int offset,
    byte[] beforeImage,
    byte[] afterImage
) implements LogRecord {
    @Override public LogRecordType type() { return LogRecordType.UPDATE; }
    
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(
            1 + 8 + 8 + 8 + 4 + 4 + 4 + beforeImage.length + afterImage.length
        );
        buf.put((byte) 1); // UPDATE type
        buf.putLong(lsn);
        buf.putLong(txnId);
        buf.putInt(pageId.fileId());
        buf.putInt(pageId.pageNumber());
        buf.putInt(offset);
        buf.putInt(beforeImage.length);
        buf.put(beforeImage);
        buf.putInt(afterImage.length);
        buf.put(afterImage);
        return buf.array();
    }
}