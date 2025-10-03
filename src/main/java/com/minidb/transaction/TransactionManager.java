package com.minidb.transaction;

import com.minidb.transaction.wal.*;
import com.minidb.transaction.lock.*;
import com.minidb.storage.buffer.BufferPool;
import com.minidb.storage.page.Page;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TransactionManager - Coordinates transactions
 */
public class TransactionManager {
    private final AtomicLong nextTxnId;
    private final Map<Long, Transaction> activeTransactions;
    private final WALManager walManager;
    private final LockManager lockManager;
    private final BufferPool bufferPool;
    
    public TransactionManager(WALManager walManager, LockManager lockManager, 
                             BufferPool bufferPool) {
        this.nextTxnId = new AtomicLong(1);
        this.activeTransactions = new ConcurrentHashMap<>();
        this.walManager = walManager;
        this.lockManager = lockManager;
        this.bufferPool = bufferPool;
    }
    
    /**
     * BEGIN transaction
     */
    public Transaction begin(IsolationLevel isolationLevel) throws IOException {
        long txnId = nextTxnId.getAndIncrement();
        Transaction txn = new Transaction(txnId, isolationLevel);
        activeTransactions.put(txnId, txn);
        
        // Write BEGIN log
        long lsn = walManager.getNextLSN();
        BeginLogRecord logRecord = new BeginLogRecord(lsn, txnId);
        walManager.appendLog(logRecord);
        
        System.out.println("Transaction " + txnId + " started");
        return txn;
    }
    
    /**
     * COMMIT transaction
     */
    public void commit(long txnId) throws IOException {
        Transaction txn = activeTransactions.get(txnId);
        if (txn == null || txn.getState() != TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction " + txnId + " is not active");
        }
        
        // Write COMMIT log
        long lsn = walManager.getNextLSN();
        CommitLogRecord logRecord = new CommitLogRecord(lsn, txnId);
        walManager.appendLog(logRecord);
        
        // NO-FORCE: Don't need to flush dirty pages immediately
        // WAL guarantees we can recover them
        
        // Release all locks
        lockManager.releaseAllLocks(txnId);
        
        txn.setState(TransactionState.COMMITTED);
        activeTransactions.remove(txnId);
        walManager.clearTransactionLogs(txnId);
        
        System.out.println("Transaction " + txnId + " committed");
    }
    
    /**
     * ABORT transaction (rollback)
     */
    public void abort(long txnId) throws IOException {
        Transaction txn = activeTransactions.get(txnId);
        if (txn == null) {
            throw new IllegalStateException("Transaction " + txnId + " not found");
        }
        
        // UNDO: Replay logs backward
        List<LogRecord> logs = walManager.getTransactionLogs(txnId);
        for (int i = logs.size() - 1; i >= 0; i--) {
            LogRecord log = logs.get(i);
            if (log instanceof UpdateLogRecord updateLog) {
                // Restore before-image
                Page page = bufferPool.fetchPage(updateLog.pageId());
                page.writeTo(updateLog.offset(), updateLog.beforeImage());
                bufferPool.unpinPage(updateLog.pageId(), true);
            }
        }
        
        // Write ABORT log
        long lsn = walManager.getNextLSN();
        AbortLogRecord logRecord = new AbortLogRecord(lsn, txnId);
        walManager.appendLog(logRecord);
        
        // Release all locks
        lockManager.releaseAllLocks(txnId);
        
        txn.setState(TransactionState.ABORTED);
        activeTransactions.remove(txnId);
        walManager.clearTransactionLogs(txnId);
        
        System.out.println("Transaction " + txnId + " aborted");
    }
    
    public Transaction getTransaction(long txnId) {
        return activeTransactions.get(txnId);
    }
}