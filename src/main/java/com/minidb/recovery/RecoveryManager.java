package com.minidb.recovery;

import com.minidb.transaction.wal.*;
import com.minidb.storage.buffer.BufferPool;
import com.minidb.storage.page.Page;

import java.io.IOException;
import java.util.*;

/**
 * Recovery Manager - ARIES-style crash recovery
 */
public class RecoveryManager {
    private final WALManager walManager;
    private final BufferPool bufferPool;
    private final CheckpointManager checkpointManager;

    public RecoveryManager(WALManager walManager, BufferPool bufferPool) {
        this.walManager = walManager;
        this.bufferPool = bufferPool;
        this.checkpointManager = new CheckpointManager(walManager, bufferPool);
    }

    /**
     * Recover from crash
     * 
     * Three phases:
     * 1. Analysis: Determine which transactions to redo/undo
     * 2. Redo: Replay all operations from committed transactions
     * 3. Undo: Rollback all uncommitted transactions
     */
    public void recover() throws IOException {
        System.out.println("=== Recovery Phase ===");

        // Phase 1: Analysis
        Set<Long> committedTxns = new HashSet<>();
        Set<Long> activeTxns = new HashSet<>();
        List<LogRecord> allLogs = new ArrayList<>();

        // Scan all logs (simplified - in reality would start from checkpoint)
        System.out.println("Phase 1: Analysis");
        for (long txnId : walManager.getAllTransactionIds()) {
            List<LogRecord> txnLogs = walManager.getTransactionLogs(txnId);
            allLogs.addAll(txnLogs);

            // Check if committed
            boolean committed = false;
            for (LogRecord log : txnLogs) {
                if (log instanceof CommitLogRecord) {
                    committed = true;
                    committedTxns.add(txnId);
                    break;
                } else if (log instanceof AbortLogRecord) {
                    committed = true; // Already aborted
                    break;
                }
            }

            if (!committed) {
                activeTxns.add(txnId);
            }
        }

        System.out.println("  - Committed transactions: " + committedTxns.size());
        System.out.println("  - Active transactions: " + activeTxns.size());

        // Phase 2: Redo
        System.out.println("Phase 2: Redo");
        int redoCount = 0;
        for (LogRecord log : allLogs) {
            if (log instanceof UpdateLogRecord update) {
                if (committedTxns.contains(log.txnId())) {
                    // Redo the update
                    redoUpdate(update);
                    redoCount++;
                }
            }
        }
        System.out.println("  - Redone operations: " + redoCount);

        // Phase 3: Undo
        System.out.println("Phase 3: Undo");
        int undoCount = 0;
        for (long txnId : activeTxns) {
            List<LogRecord> txnLogs = walManager.getTransactionLogs(txnId);

            // Undo in reverse order
            for (int i = txnLogs.size() - 1; i >= 0; i--) {
                LogRecord log = txnLogs.get(i);
                if (log instanceof UpdateLogRecord update) {
                    undoUpdate(update);
                    undoCount++;
                }
            }
        }
        System.out.println("  - Undone operations: " + undoCount);

        System.out.println("Recovery complete.\n");
    }

    private void redoUpdate(UpdateLogRecord update) throws IOException {
        // Apply after-image
        Page page = bufferPool.fetchPage(update.pageId());
        page.writeTo(update.offset(), update.afterImage());
        bufferPool.unpinPage(update.pageId(), true);
    }

    private void undoUpdate(UpdateLogRecord update) throws IOException {
        // Apply before-image
        Page page = bufferPool.fetchPage(update.pageId());
        page.writeTo(update.offset(), update.beforeImage());
        bufferPool.unpinPage(update.pageId(), true);
    }

    /**
     * Create checkpoint
     */
    public void checkpoint() throws IOException {
        checkpointManager.createCheckpoint();
    }
}
