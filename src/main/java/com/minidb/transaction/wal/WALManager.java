package com.minidb.transaction.wal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WALManager - Write-Ahead Log manager
 */
public class WALManager {
    private final Path walPath;
    private final FileChannel walChannel;
    private final AtomicLong nextLSN;
    private final ReentrantLock writeLock;
    private final Map<Long, List<LogRecord>> txnLogs; // For undo

    public WALManager(String walFilePath) throws IOException {
        this.walPath = Paths.get(walFilePath);
        RandomAccessFile raf = new RandomAccessFile(walPath.toFile(), "rw");
        this.walChannel = raf.getChannel();
        this.nextLSN = new AtomicLong(0);
        this.writeLock = new ReentrantLock();
        this.txnLogs = new ConcurrentHashMap<>();
    }

    /**
     * Append log record to WAL
     */
    public long appendLog(LogRecord record) throws IOException {
        writeLock.lock();
        try {
            byte[] serialized = record.serialize();
            ByteBuffer buffer = ByteBuffer.allocate(4 + serialized.length);
            buffer.putInt(serialized.length);
            buffer.put(serialized);
            buffer.flip();

            walChannel.write(buffer);
            walChannel.force(true); // Force to disk (durability!)

            // Store for potential undo
            txnLogs.computeIfAbsent(record.txnId(), k -> new ArrayList<>())
                    .add(record);

            return record.lsn();
        } finally {
            writeLock.unlock();
        }
    }

    public long getNextLSN() {
        return nextLSN.getAndIncrement();
    }

    public List<LogRecord> getTransactionLogs(long txnId) {
        return txnLogs.getOrDefault(txnId, Collections.emptyList());
    }

    public void clearTransactionLogs(long txnId) {
        txnLogs.remove(txnId);
    }

    public Set<Long> getAllTransactionIds() {
        return txnLogs.keySet();
    }

    public void close() throws IOException {
        walChannel.close();
    }
}