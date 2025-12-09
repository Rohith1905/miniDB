package com.minidb.recovery;

import com.minidb.transaction.wal.WALManager;
import com.minidb.storage.buffer.BufferPool;

import java.io.IOException;

/**
 * Checkpoint Manager - Reduces recovery time
 */
public class CheckpointManager {
    private final WALManager walManager;
    private final BufferPool bufferPool;
    
    public CheckpointManager(WALManager walManager, BufferPool bufferPool) {
        this.walManager = walManager;
        this.bufferPool = bufferPool;
    }
    
    /**
     * Create checkpoint:
     * 1. Flush all dirty pages to disk
     * 2. Write checkpoint record to WAL
     * 3. Sync WAL
     */
    public void createCheckpoint() throws IOException {
        System.out.println("Creating checkpoint...");
        
        // 1. Flush all dirty pages
        bufferPool.flushAllPages();
        
        // 2. Write checkpoint record (simplified)
        long lsn = walManager.getNextLSN();
        // In real system, would write CheckpointLogRecord with active transactions
        
        System.out.println("Checkpoint created at LSN " + lsn);
    }
}