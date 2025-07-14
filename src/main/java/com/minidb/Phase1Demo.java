package com.minidb;

import com.minidb.storage.page.*;
import com.minidb.storage.buffer.*;
import com.minidb.transaction.*;
import com.minidb.transaction.wal.*;
import com.minidb.transaction.lock.*;

public class Phase1Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MiniDB Phase 1 Demo ===\n");
        // Initialize components
        PageManager pageManager = new PageManager("./data");
        BufferPool bufferPool = new BufferPool(10, pageManager);
        WALManager walManager = new WALManager("./data/wal.log");
        LockManager lockManager = new LockManager();
        TransactionManager txnManager = new TransactionManager(
                walManager, lockManager, bufferPool);

        // Demo 1: Basic transaction
        System.out.println("--- Demo 1: Basic Transaction ---");
        Transaction txn1 = txnManager.begin(IsolationLevel.SERIALIZABLE);

        // Allocate a page
        PageId pageId = pageManager.allocatePage(1);
        System.out.println("Allocated: " + pageId);

        // Acquire lock and write data
        lockManager.acquireLock(txn1.getTxnId(), pageId.toString(),
                LockMode.EXCLUSIVE, 5000);

        Page page = bufferPool.fetchPage(pageId);
        page.writeTo(0, "Hello MiniDB!".getBytes());

        // Log the update
        long lsn = walManager.getNextLSN();
        UpdateLogRecord updateLog = new UpdateLogRecord(
                lsn, txn1.getTxnId(), pageId, 0,
                new byte[0], "Hello MiniDB!".getBytes());
        walManager.appendLog(updateLog);

        bufferPool.unpinPage(pageId, true);
        txnManager.commit(txn1.getTxnId());

        System.out.println("\n--- Demo 2: Concurrent Transactions ---");

        // Txn 2 tries to read (should succeed with shared lock)
        Transaction txn2 = txnManager.begin(IsolationLevel.READ_COMMITTED);
        lockManager.acquireLock(txn2.getTxnId(), pageId.toString(),
                LockMode.SHARED, 5000);
        Page page2 = bufferPool.fetchPage(pageId);
        byte[] data = page2.readFrom(0, 13);
        System.out.println("Txn2 read: " + new String(data));
        bufferPool.unpinPage(pageId, false);
        lockManager.releaseAllLocks(txn2.getTxnId());
        txnManager.commit(txn2.getTxnId());

        // Cleanup
        bufferPool.flushAllPages();
        walManager.close();
        pageManager.close();

        System.out.println("\n✅ Phase 1 Complete!");
    }
}